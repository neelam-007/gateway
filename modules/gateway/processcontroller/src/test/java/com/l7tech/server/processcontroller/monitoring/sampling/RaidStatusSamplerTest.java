package com.l7tech.server.processcontroller.monitoring.sampling;

import org.junit.*;
import static org.junit.Assert.*;
import com.l7tech.server.management.api.monitoring.RaidStatus;

/**
 *
 */
public class RaidStatusSamplerTest {

    public static final String OUTPUT_NOTRAID =
            "Personalities :\n" +
            "unused devices: <none>";

    public static final String OUTPUT_OK =
            "Personalities : [raid6] [raid5] [raid4] [raid1]\n" +
            "md0 : active raid1 sda1[0] sdb1[1]\n" +
            "      104320 blocks [2/2] [UU]\n" +
            "\n" +
            "md1 : active raid5 sda2[0] sdd2[3] sdc2[2] sdb2[1]\n" +
            "      875943168 blocks level 5, 256k chunk, algorithm 2 [4/4] [UUUU]\n" +
            "\n" +
            "unused devices: <none>";

    public static final String OUTPUT_BAD =
            "Personalities : [raid6] [raid5] [raid4] [raid1]\n" +
            "md0 : active raid1 sda1[0] sdb1[1]\n" +
            "      104320 blocks [2/2] [UU]\n" +
            "\n" +
            "md1 : active raid5 sda2[0] sdd2[3] sdc2[2] sdb2[1]\n" +
            "      875943168 blocks level 5, 256k chunk, algorithm 2 [4/4] [U__U]\n" +
            "\n" +
            "unused devices: <none>";

    public static final String OUTPUT_REBUILDING =
            "Personalities : [raid1] [raid6] [raid5] [raid4]\n" +
            "md127 : active raid5 sdh1[6] sdg1[4] sdf1[3] sde1[2] sdd1[1] sdc1[0]\n" +
            "      1464725760 blocks level 5, 64k chunk, algorithm 2 [6/5] [UUUUU_]\n" +
            "      [==>..................]  recovery = 12.6% (37043392/292945152) finish=127.5min speed=33440K/sec\n" +
            "\n" +
            "unused devices: <none>";

    @Test
    public void testMatch() throws Exception {
        assertEquals(RaidStatus.NOT_RAID, RaidStatusSampler.parseOutput(OUTPUT_NOTRAID));
        assertEquals(RaidStatus.OK, RaidStatusSampler.parseOutput(OUTPUT_OK));
        assertEquals(RaidStatus.BAD,  RaidStatusSampler.parseOutput(OUTPUT_BAD));
        assertEquals(RaidStatus.REBUILDING, RaidStatusSampler.parseOutput(OUTPUT_REBUILDING));
    }
}
