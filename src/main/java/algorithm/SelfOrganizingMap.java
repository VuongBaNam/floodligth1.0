package algorithm;

import algorithm.distance.DistanceMethodInterface;
import algorithm.distance.EuclideanDistanceMethod;
import algorithm.exception.SOMException;
import algorithm.matrix.SampleVectorInterface;
import algorithm.matrix.WeightMatrix;
import algorithm.matrix.vo.SOMNode;
import algorithm.matrix.vo.WeightElementVO;
import algorithm.neighbors.GaussianNeighborsMethod;
import algorithm.neighbors.NeighborsMethodInterface;

import java.util.List;

public class SelfOrganizingMap {
  private enum Status {START, STOP, PAUSE};
  private Status status;
  private WeightMatrix weightMatrix;
  private SampleVectorInterface sampleVector;
  private double t;
  private int iteration;
  private int iterationNumber;
  private double tPerIteration;

  public SelfOrganizingMap(int iterationNumber, SampleVectorInterface sampleVector)
          throws SOMException {
    if (sampleVector == null)
      throw new SOMException("Invalid sample vector");

    if (iterationNumber < 1)
      throw new SOMException("Invalid Iteration Number");

    setIterationNumber(iterationNumber);

    this.weightMatrix    = null;
    this.sampleVector    = sampleVector;
    status    = Status.STOP;
    iteration = 0;
  }

  public void setIterationNumber(int iterationNumber)
          throws SOMException {
    if (status == Status.START)
      throw new SOMException("The process is already started");

    this.iterationNumber = iterationNumber;

    iteration     = 0;
    t             = 0.0d;
    tPerIteration = 1.0d/iterationNumber;
  }

  public int getIterationNumber() {
    return iterationNumber;
  }

  protected final void startLearn() throws SOMException {
    if (status == Status.START)
      throw new SOMException("The process is already started");

    status = Status.START;
    while (t <= 1.0d && status == Status.START)
      stepLearn();

    if (status != Status.PAUSE) {
      if (iteration != 0)
        emitEnd();
    }
  }

  private final void emitEnd() throws SOMException {
    status = Status.STOP;
    setIterationNumber(this.iterationNumber);
  }

  public final void startLearn(int weightWidth, int weightHeight)
          throws SOMException {
    startLearn(weightWidth, weightHeight, new EuclideanDistanceMethod(),
            new GaussianNeighborsMethod());
  }

  public final void startLearn(int weightWidth, int weightHeight, DistanceMethodInterface distanceMethod,
                               NeighborsMethodInterface neighborsMethod) throws SOMException {
    weightMatrix = new WeightMatrix(weightWidth, weightHeight,
            sampleVector, neighborsMethod, distanceMethod);
    startLearn();
  }

  public final void stepLearn() throws SOMException {
    if (weightMatrix == null)
      throw new SOMException("Weight Matrix not initialized");

    if (t > 1.0d) {
      if (iteration != 0)
        emitEnd();

      return;
    }

    weightMatrix.executeStepLearn(t);
    t += tPerIteration;
    iteration++;

    if (t > 1.0) {
      if (iteration != 0)
        emitEnd();
    }
  }

  public final void stepLearn(int weightWidth, int weightHeight)
          throws SOMException {
    stepLearn(weightWidth, weightHeight, new EuclideanDistanceMethod(),
            new GaussianNeighborsMethod());
  }

  public final void stepLearn(int weightWidth, int weightHeight,
                              DistanceMethodInterface distanceMethod, NeighborsMethodInterface
                                      neighborsMethod) throws SOMException {
    weightMatrix = new WeightMatrix(weightWidth, weightHeight,
            sampleVector, neighborsMethod, distanceMethod);

    stepLearn();
  }

  public double classify(SOMNode input) throws SOMException {

    List<WeightElementVO> list = weightMatrix.getKBestWeight(weightMatrix.getMatrix(),input);
    for (WeightElementVO weightElementVO : list) {
      int dem = 0;
      if (weightElementVO.getGroup() == null) {
        continue;
      }
      for (SOMNode somNode : weightElementVO.getGroup()) {
        if (somNode.getCategory() == Double.valueOf(1)) {
          dem++;
        }
      }
      if (dem > weightElementVO.getGroup().size() / 2) {
        return 1;
      } else if (dem < weightElementVO.getGroup().size() / 2) {
        return 0;
      }
    }
    return 0.0d;
  }

  public final void pauseLearn() throws SOMException {
    if (status == Status.STOP)
      throw new SOMException("The process is not started");

    status = Status.PAUSE;
  }

  public final WeightElementVO[][] mountGroups() throws SOMException {
    return mountGroups(sampleVector);
  }

  public final WeightElementVO[][] mountGroups(SampleVectorInterface
                                                       sampleVector) throws SOMException {
    if (status == Status.START)
      throw new SOMException("The process is already started");

    if (weightMatrix == null)
      throw new SOMException("The process was not started");

    if (sampleVector == null)
      throw new SOMException("The sample vector must not be empty");

    return weightMatrix.mountGroups(sampleVector);
  }

  public final SampleVectorInterface getSample() {
    return sampleVector;
  }

  public WeightMatrix getWeightMatrix() {
    return weightMatrix;
  }

  public void setWeightMatrix(WeightMatrix weightMatrix) {
    this.weightMatrix = weightMatrix;
  }

  public SampleVectorInterface getSampleVector() {
    return sampleVector;
  }

  public void setSampleVector(SampleVectorInterface sampleVector) {
    this.sampleVector = sampleVector;
  }
}