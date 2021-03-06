package net.floodlightcontroller.statistics;

import algorithm.SelfOrganizingMap;
import algorithm.exception.SOMException;
import algorithm.matrix.SampleVectorFile;
import algorithm.matrix.SampleVectorInterface;
import algorithm.matrix.vo.SOMNode;
import algorithm.matrix.vo.WeightElementVO;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ListenableFuture;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.statistics.web.SwitchStatisticsWebRoutable;
import net.floodlightcontroller.test.*;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.ver13.OFMeterSerializerVer13;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.Thread.State;
import java.net.Socket;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StatisticsCollector implements IFloodlightModule, IStatisticsService {
	private static final Logger log = LoggerFactory.getLogger(StatisticsCollector.class);

	private static IOFSwitchService switchService;
	private static IThreadPoolService threadPoolService;
	private static IRestApiService restApiService;

	private static boolean isEnabled = false;

	private static int portStatsInterval = 10; /* could be set by REST API, so not final */
	private static ScheduledFuture<?> portStatsCollector;
	private static ScheduledFuture<?> flowStatsCollector;

	private static final long BITS_PER_BYTE = 8;
	private static final long MILLIS_PER_SEC = 1000;
	
	private static final String INTERVAL_PORT_STATS_STR = "collectionIntervalPortStatsSeconds";
	private static final String ENABLED_STR = "enable";

	private static final HashMap<NodePortTuple, SwitchPortBandwidth> portStats = new HashMap<NodePortTuple, SwitchPortBandwidth>();
	private static final HashMap<NodePortTuple, SwitchPortBandwidth> tentativePortStats = new HashMap<NodePortTuple, SwitchPortBandwidth>();

	public static final int iterationNumber = 20;
	private Map<DatapathId,Item> matrix = new HashMap<DatapathId,Item>();
	private Map<DatapathId,List<Item>> listFlow1 = new HashMap<DatapathId,List<Item>>();
	private Map<DatapathId,List<Item>> listFlow2 = new HashMap<DatapathId,List<Item>>();

	private SelfOrganizingMap som;

	int dem = 1;
	/**
	 * Run periodically to collect all port statistics. This only collects
	 * bandwidth stats right now, but it could be expanded to record other
	 * information as well. The difference between the most recent and the
	 * current RX/TX bytes is used to determine the "elapsed" bytes. A 
	 * timestamp is saved each time stats results are saved to compute the
	 * bits per second over the elapsed time. There isn't a better way to
	 * compute the precise bandwidth unless the switch were to include a
	 * timestamp in the stats reply message, which would be nice but isn't
	 * likely to happen. It would be even better if the switch recorded 
	 * bandwidth and reported bandwidth directly.
	 * 
	 * Stats are not reported unless at least two iterations have occurred
	 * for a single switch's reply. This must happen to compare the byte 
	 * counts and to get an elapsed time.
	 * 
	 * @author Ryan Izard, ryan.izard@bigswitch.com, rizard@g.clemson.edu
	 *
	 */
	private class FlowStatsCollector implements Runnable{

		public Item createItem(OFFlowStatsEntry fse){
			Item item = new Item();
			Match match = fse.getMatch();

			item.setAttribute(Flow.IP_SRC.toString(),match.get(MatchField.IPV4_SRC));
			item.setAttribute(Flow.IP_DST.toString(),match.get(MatchField.IPV4_DST));
			if(match.get(MatchField.IP_PROTO) == null){
				item.setAttribute(Flow.PORT_SRC.toString(),TransportPort.NONE);
				item.setAttribute(Flow.PORT_DST.toString(),TransportPort.NONE);
			}else if (match.get(MatchField.IP_PROTO).equals(IpProtocol.TCP)){
				item.setAttribute(Flow.PORT_SRC.toString(), match.get(MatchField.TCP_SRC));
				item.setAttribute(Flow.PORT_DST.toString(), match.get(MatchField.TCP_DST));
			}else{
				item.setAttribute(Flow.PORT_SRC.toString(), match.get(MatchField.UDP_SRC));
				item.setAttribute(Flow.PORT_DST.toString(), match.get(MatchField.UDP_DST));
			}
			item.setAttribute(Flow.NUMBER_PKT.toString(),fse.getPacketCount());
			item.setAttribute(Flow.TOATAL_BYTE.toString(),fse.getByteCount());

			return item;
		}

		public Item getItem(List<Item> list , Item item){
			for(Item i : list){
				if(compareFlow(i,item)){
					return i;
				}
			}
			return null;
		}

		public boolean compareFlow(Item flow1,Item flow2){
			if(flow1.getFieldValue(Flow.IP_SRC.toString()).equals(flow2.getFieldValue(Flow.IP_SRC.toString()))
					&& flow1.getFieldValue(Flow.IP_DST.toString()).equals(flow2.getFieldValue(Flow.IP_DST.toString()))
					&& flow1.getFieldValue(Flow.PORT_SRC.toString()).equals(flow2.getFieldValue(Flow.PORT_SRC.toString()))
					&& flow1.getFieldValue(Flow.PORT_DST.toString()).equals(flow2.getFieldValue(Flow.PORT_DST.toString()))){
				return true;
			}else
				return false;
		}

		@Override
		public void run() {
			sendFlowDeleteMessage();
//			System.out.println(dem);
//			dem++;
//			List<Item> list1 = new ArrayList<>();
//			List<Item> list2 = new ArrayList<>();
//			Map<DatapathId, List<OFStatsReply>> replies = getSwitchStatistics(switchService.getAllSwitchDpids(),OFStatsType.FLOW);
//			for (Map.Entry<DatapathId, List<OFStatsReply>> e : replies.entrySet()) {
//				long numberByte = 0;
//				long numberPacket = 0;
//
//				if(listFlow1.get(e.getKey()) != null){
//					list1 = listFlow1.get(e.getKey());
//					list2 = listFlow2.get(e.getKey());
//				}
//
//				for (OFStatsReply r : e.getValue()) {
//					OFFlowStatsReply fsr = (OFFlowStatsReply) r;
//					for (OFFlowStatsEntry fse : fsr.getEntries()) {
//						if (fse.getMatch().get(MatchField.ETH_TYPE) == EthType.ARP) continue;
//						Item flowEntry = createItem(fse);
//						System.out.println(e.getKey()+"\t"+flowEntry.getFieldValue(Flow.IP_SRC.toString()) +"\t"+flowEntry.getFieldValue(Flow.IP_DST.toString()));
//						list2.add(flowEntry);
//						if(list1.isEmpty()) {
//							numberByte += fse.getByteCount().getValue();
//							numberPacket += fse.getPacketCount().getValue();
//						}else {
//							Item i = getItem(list1,flowEntry);
//							if(i == null){
//								U64 total_byte = (U64)flowEntry.getFieldValue(Flow.TOATAL_BYTE.toString());
//								U64 total_pkt = (U64)flowEntry.getFieldValue(Flow.NUMBER_PKT.toString());
//								numberPacket += total_pkt.getValue();
//								numberByte += total_byte.getValue();
//							}else{
//								U64 total_b = (U64)flowEntry.getFieldValue(Flow.TOATAL_BYTE.toString());
//								U64 total_p = (U64)flowEntry.getFieldValue(Flow.NUMBER_PKT.toString());
//								U64 total_byte = (U64)i.getFieldValue(Flow.TOATAL_BYTE.toString());
//								U64 total_pkt = (U64)i.getFieldValue(Flow.NUMBER_PKT.toString());
//								numberPacket += total_p.getValue() - total_pkt.getValue();
//								numberByte += total_b.getValue() - total_byte.getValue();
//							}
//						}
//					}
//				}
//				list1.clear();
//				list1 = new ArrayList<>(list2);
//				list2.clear();
//				Item item = new Item();
//				item.setAttribute(Parameters.BYTE_COUNT.toString(),numberByte);
//				item.setAttribute(Parameters.PACKET_COUNT.toString(),numberPacket);
//				matrix.put(e.getKey(),item);
//				listFlow1.put(e.getKey(),list1);
//				listFlow2.put(e.getKey(),list2);
//			}
//			System.out.println("Datapath ID \t byte_count \t packet count");
//			for(Map.Entry<DatapathId, Item> e : matrix.entrySet()){
//				System.out.println(e.getKey() +"\t"+e.getValue().getFieldValue(Parameters.BYTE_COUNT.toString())
//						+"\t"+e.getValue().getFieldValue(Parameters.PACKET_COUNT.toString()));
//			}
		}
		private void sendFlowDeleteMessage() {
			Map<DatapathId, List<OFStatsReply>> replies = getSwitchStatistics(switchService.getAllSwitchDpids(),OFStatsType.FLOW);
			int numberFlowDeleted = 0;
			int numberFlow = 0;
			for (Map.Entry<DatapathId, List<OFStatsReply>> e : replies.entrySet()) {
				for (OFStatsReply r : e.getValue()) {
					OFFlowStatsReply fsr = (OFFlowStatsReply) r;
					List<OFFlowStatsEntry> list = fsr.getEntries();
					numberFlow = list.size();
					for (OFFlowStatsEntry fse : list) {
						Match match = fse.getMatch();

						if(fse.getPacketCount().getValue() != 0){
							IOFSwitch sw = switchService.getSwitch(e.getKey());

							OFFlowDelete flowDelete = sw.getOFFactory().buildFlowDelete()
									.setOutPort(OFPort.ANY)
									.setMatch(match).build();
							sw.write(flowDelete);
							numberFlowDeleted++;
						}
					}
				}
			}
		}
	}


	/**
	 * Single thread for collecting switch statistics and
	 * containing the reply.
	 * 
	 * @author Ryan Izard, ryan.izard@bigswitch.com, rizard@g.clemson.edu
	 *
	 */
	private class GetStatisticsThread extends Thread {
		private List<OFStatsReply> statsReply;
		private DatapathId switchId;
		private OFStatsType statType;

		public GetStatisticsThread(DatapathId switchId, OFStatsType statType) {
			this.switchId = switchId;
			this.statType = statType;
			this.statsReply = null;
		}

		public List<OFStatsReply> getStatisticsReply() {
			return statsReply;
		}

		public DatapathId getSwitchId() {
			return switchId;
		}

		@Override
		public void run() {
			statsReply = getSwitchStatistics(switchId, statType);
		}
	}
	
	/*
	 * IFloodlightModule implementation
	 */
	
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IStatisticsService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m =
				new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IStatisticsService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IOFSwitchService.class);
		l.add(IThreadPoolService.class);
		l.add(IRestApiService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException, IOException {
		switchService = context.getServiceImpl(IOFSwitchService.class);
		threadPoolService = context.getServiceImpl(IThreadPoolService.class);
		restApiService = context.getServiceImpl(IRestApiService.class);

//		client = new Socket("localhost",9999);

		Map<String, String> config = context.getConfigParams(this);
		if (config.containsKey(ENABLED_STR)) {
			try {
				isEnabled = Boolean.parseBoolean(config.get(ENABLED_STR).trim());
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", ENABLED_STR, isEnabled);
			}
		}
		log.info("Statistics collection {}", isEnabled ? "enabled" : "disabled");

		if (config.containsKey(INTERVAL_PORT_STATS_STR)) {
			try {
				portStatsInterval = Integer.parseInt(config.get(INTERVAL_PORT_STATS_STR).trim());
			} catch (Exception e) {
				log.error("Could not parse '{}'. Using default of {}", INTERVAL_PORT_STATS_STR, portStatsInterval);
			}
		}
		log.info("Port statistics collection interval set to {}s", portStatsInterval);
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		restApiService.addRestletRoutable(new SwitchStatisticsWebRoutable());
		if (isEnabled) {
			startStatisticsCollection();
		}
	}

	/*
	 * IStatisticsService implementation
	 */
	
	@Override
	public SwitchPortBandwidth getBandwidthConsumption(DatapathId dpid, OFPort p) {
		return portStats.get(new NodePortTuple(dpid, p));
	}
	

	@Override
	public Map<NodePortTuple, SwitchPortBandwidth> getBandwidthConsumption() {
		return Collections.unmodifiableMap(portStats);
	}

	@Override
	public synchronized void collectStatistics(boolean collect) {
		if (collect && !isEnabled) {
			startStatisticsCollection();
			isEnabled = true;
		} else if (!collect && isEnabled) {
			stopStatisticsCollection();
			isEnabled = false;
		} 
		/* otherwise, state is not changing; no-op */
	}
	
	/*
	 * Helper functions
	 */
	
	/**
	 * Start all stats threads.
	 */
	private void startStatisticsCollection() {
		//portStatsCollector = threadPoolService.getScheduledExecutor().scheduleAtFixedRate(new PortStatsCollector(), 5, 5, TimeUnit.SECONDS);
		//flowStatsCollector = threadPoolService.getScheduledExecutor().scheduleAtFixedRate(new FlowStatsCollector(), 2, 2, TimeUnit.SECONDS);
		tentativePortStats.clear(); /* must clear out, otherwise might have huge BW result if present and wait a long time before re-enabling stats */
		log.warn("Statistics collection thread(s) started");
	}
	
	/**
	 * Stop all stats threads.
	 */
	private void stopStatisticsCollection() {
		if (!portStatsCollector.cancel(false) && !flowStatsCollector.cancel(false)) {
			log.error("Could not cancel port stats thread and flow stats");
		} else {
			log.warn("Statistics collection thread(s) stopped");
		}
	}

	
	/**
	 * Retrieve the statistics from all switches in parallel.
	 * @param dpids
	 * @param statsType
	 * @return
	 */
	private Map<DatapathId, List<OFStatsReply>> getSwitchStatistics(Set<DatapathId> dpids, OFStatsType statsType) {
		HashMap<DatapathId, List<OFStatsReply>> model = new HashMap<DatapathId, List<OFStatsReply>>();

		List<GetStatisticsThread> activeThreads = new ArrayList<GetStatisticsThread>(dpids.size());
		List<GetStatisticsThread> pendingRemovalThreads = new ArrayList<GetStatisticsThread>();
		GetStatisticsThread t;
		for (DatapathId d : dpids) {
			t = new GetStatisticsThread(d, statsType);
			activeThreads.add(t);
			t.start();
		}

		/* Join all the threads after the timeout. Set a hard timeout
		 * of 12 seconds for the threads to finish. If the thread has not
		 * finished the switch has not replied yet and therefore we won't
		 * add the switch's stats to the reply.
		 */
		for (int iSleepCycles = 0; iSleepCycles < portStatsInterval; iSleepCycles++) {
			for (GetStatisticsThread curThread : activeThreads) {
				if (curThread.getState() == State.TERMINATED) {
					model.put(curThread.getSwitchId(), curThread.getStatisticsReply());
					pendingRemovalThreads.add(curThread);
				}
			}

			/* remove the threads that have completed the queries to the switches */
			for (GetStatisticsThread curThread : pendingRemovalThreads) {
				activeThreads.remove(curThread);
			}
			
			/* clear the list so we don't try to double remove them */
			pendingRemovalThreads.clear();

			/* if we are done finish early */
			if (activeThreads.isEmpty()) {
				break;
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.error("Interrupted while waiting for statistics", e);
			}
		}

		return model;
	}

	/**
	 * Get statistics from a switch.
	 * @param switchId
	 * @param statsType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<OFStatsReply> getSwitchStatistics(DatapathId switchId, OFStatsType statsType) {
		IOFSwitch sw = switchService.getSwitch(switchId);
		ListenableFuture<?> future;
		List<OFStatsReply> values = null;
		Match match;
		if (sw != null) {
			OFStatsRequest<?> req = null;
			switch (statsType) {
			case FLOW:
				match = sw.getOFFactory().buildMatch().build();
				req = sw.getOFFactory().buildFlowStatsRequest()
						.setMatch(match)
						.setOutPort(OFPort.ANY)
						.setTableId(TableId.ALL)
						.build();
				break;
			case AGGREGATE:
				match = sw.getOFFactory().buildMatch().build();
				req = sw.getOFFactory().buildAggregateStatsRequest()
						.setMatch(match)
						.setOutPort(OFPort.ANY)
						.setTableId(TableId.ALL)
						.build();
				break;
			case PORT:
				req = sw.getOFFactory().buildPortStatsRequest()
				.setPortNo(OFPort.ANY)
				.build();
				break;
			case QUEUE:
				req = sw.getOFFactory().buildQueueStatsRequest()
				.setPortNo(OFPort.ANY)
				.setQueueId(UnsignedLong.MAX_VALUE.longValue())
				.build();
				break;
			case DESC:
				req = sw.getOFFactory().buildDescStatsRequest()
				.build();
				break;
			case GROUP:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildGroupStatsRequest()				
							.build();
				}
				break;

			case METER:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildMeterStatsRequest()
							.setMeterId(OFMeterSerializerVer13.ALL_VAL)
							.build();
				}
				break;

			case GROUP_DESC:			
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildGroupDescStatsRequest()			
							.build();
				}
				break;

			case GROUP_FEATURES:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildGroupFeaturesStatsRequest()
							.build();
				}
				break;

			case METER_CONFIG:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildMeterConfigStatsRequest()
							.build();
				}
				break;

			case METER_FEATURES:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildMeterFeaturesStatsRequest()
							.build();
				}
				break;

			case TABLE:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildTableStatsRequest()
							.build();
				}
				break;

			case TABLE_FEATURES:	
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_10) > 0) {
					req = sw.getOFFactory().buildTableFeaturesStatsRequest()
							.build();		
				}
				break;
			case PORT_DESC:
				if (sw.getOFFactory().getVersion().compareTo(OFVersion.OF_13) >= 0) {
					req = sw.getOFFactory().buildPortDescStatsRequest()
							.build();
				}
				break;
			case EXPERIMENTER:		
			default:
				log.error("Stats Request Type {} not implemented yet", statsType.name());
				break;
			}

			try {
				if (req != null) {
					future = sw.writeStatsRequest(req);
					values = (List<OFStatsReply>) future.get(portStatsInterval / 2, TimeUnit.SECONDS);
				}
			} catch (Exception e) {
				log.error("Failure retrieving statistics from switch {}. {}", sw, e);
				return new ArrayList<OFStatsReply>();
			}
		}
		return values;
	}


}
