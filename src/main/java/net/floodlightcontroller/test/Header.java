package net.floodlightcontroller.test;

import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.U64;

public class Header {
    U64 cookie;
    U64 packet_count;
    Match macth;

    public Header(U64 cookie, U64 packet_count, Match macth) {
        this.cookie = cookie;
        this.packet_count = packet_count;
        this.macth = macth;
    }

    public U64 getCookie() {
        return cookie;
    }

    public void setCookie(U64 cookie) {
        this.cookie = cookie;
    }

    public U64 getPacket_count() {
        return packet_count;
    }

    public void setPacket_count(U64 packet_count) {
        this.packet_count = packet_count;
    }

    public Match getMacth() {
        return macth;
    }

    public void setMacth(Match macth) {
        this.macth = macth;
    }
}
