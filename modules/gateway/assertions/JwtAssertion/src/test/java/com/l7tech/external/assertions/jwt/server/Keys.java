package com.l7tech.external.assertions.jwt.server;


public final class Keys {

    public static final String MAC_SECRET = "hJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYg";

    public static final String SAMPLE_CERTIFICATE = "-----BEGIN CERTIFICATE-----\n" +
            "MIIC5jCCAc6gAwIBAgIILbdDnNsYEBwwDQYJKoZIhvcNAQEMBQAwETEPMA0GA1UEAxMGZm9vYmFy\n" +
            "MB4XDTE0MTIwMjIyNDk1NloXDTE5MTIwMTIyNDk1NlowETEPMA0GA1UEAxMGZm9vYmFyMIIBIjAN\n" +
            "BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAviixV9/+P+pWotLwCnNzoLvVoMGVO7hYl0fLDWbs\n" +
            "hQFwwM59KXRsSnmn1DvVTCOfwCdBg3s+agd3rgBQ8ZkHxn2/ldw8RSRl2y+s/tpJfcUIdn186RW2\n" +
            "dfR5pY6R/W7qi8RZKeniI+PDtpAuDi4TvP/bIF9dyuJNM6Zm2i1UtGHDMmjiCY9jJT8Ia9RsJ+Ic\n" +
            "MtLzvULuiCMcp/01NNqwuV4o93BucseuIPb5Me1X1KZRe/Adha0Mn7mCBhLvkDZwbf/WR9YyoZTt\n" +
            "F9iUzyKcplNkJSxNBA6U8VYxLx/RT9Kxknms8QQPHLsdxkS5W8LQa3pAlAD+N4MPOrmaE92VxwID\n" +
            "AQABo0IwQDAdBgNVHQ4EFgQU5siUh9vUM5aeydn6PtNB1LD0Xa4wHwYDVR0jBBgwFoAU5siUh9vU\n" +
            "M5aeydn6PtNB1LD0Xa4wDQYJKoZIhvcNAQEMBQADggEBAJDTLanB/aOZGjfADBLXL5Bsj70HfNNc\n" +
            "NAYVqfwqStDPhI2WyLXJKrrZvVTVY0pqQDLrgl9LZSVTBCyobynaBbLbd8IfHnfqV1Bm3SqAAo7d\n" +
            "C8VOI22KduLpkPctJC1pJqtU33ciQBDbelEJO8xrlT3PtoEVxfkeJRjue/7WYqls1e9xX827x1vz\n" +
            "V3ZmazwV8RqRL3VF4PXCpDc+rJQq1naZVZqijMhXv85uzPRJ8z+NNB5eN+VP5+tEudEBVSfv1pyY\n" +
            "1lG7bXnA+33SvfdGmkVbmCk7ZGaBlW2fa0lWJAhegEvAqDAlS+uKVhs7bDei1ij/gtc2gtmmxY1n\n" +
            "CrEerhQ=\n" +
            "-----END CERTIFICATE-----";

    public static final String SAMPLE_VERIFY_CERT = "-----BEGIN CERTIFICATE-----\n"+
            "MIIDAjCCAeqgAwIBAgIIcNqt66fBDGwwDQYJKoZIhvcNAQEMBQAwHzEdMBsGA1UEAxMUZGlla2Uw\n"+
            "Mi0wMTEwOC5jYS5jb20wHhcNMTQxMjI5MTcxNjIyWhcNMjQxMjI2MTcxNjIyWjAfMR0wGwYDVQQD\n"+
            "ExRkaWVrZTAyLTAxMTA4LmNhLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJ4F\n"+
            "T5ocaf9dhEEh7HVLEJvUn2NOx+95F2TnHuLDSKJMerpgescjhQuzvzYp29AbajFHaDMvNBUc0Mz9\n"+
            "psegEMC9z9i/VFFTNyTDRNRK+QIIADsNjAq/hPuG0ScYHF/cFTNykBlO60wiNIJXA7CynlrFIqe3\n"+
            "EenkvHHD0LHHh6a5+CFLLoJ1F97p31LpXscryNvoBgS73wKXrxr+CkwiteVxSAnfwcVRQjWhWbVW\n"+
            "iUImp2wwFgZh4vDsQR+g+Et9vatLhXAh6I1wRYkyKXJ3u8FhFsKMoAvGDnAMiy+ZcZ2rQ+4NH900\n"+
            "ax31VBLEr7tIPwkRPU5MUutjm7XqX++Lth0CAwEAAaNCMEAwHQYDVR0OBBYEFJJIKXeaoJdDK8ES\n"+
            "bErkRzjgxk3uMB8GA1UdIwQYMBaAFJJIKXeaoJdDK8ESbErkRzjgxk3uMA0GCSqGSIb3DQEBDAUA\n"+
            "A4IBAQB8Rw4xVAABSBRM9ZLVSvzbInaAcC+ZTnzt+aSZOI4bHdqz9038ZdZiZUTpdrzPE39hIjFw\n"+
            "3BKuEo/sTHQgHviUDp6rSHk2/TTFMpHNQfD2pnBs0jk3RT9NUYm5YI9Lvu4XqA106SBvVxsFBVWJ\n"+
            "tJMHNxBD9XTPSYo96HwwcStTHNL+Ga6tk6pUwHqOHVOGOlAsMLwKeZaDmwk5iIOf+NkISAKfz/jM\n"+
            "0fozOuZBZrqrNVumGCuo5cxV1HOOpfJDg+4XDfHGYMd5qI9YI7VVsXMirokbxs/ieJu8JoE7BKwn\n"+
            "ih50qtU+4Lqv5Ikge522ZrEFbPV5+IjQ4Eiat9+WXJFa\n"+
            "-----END CERTIFICATE-----";

    public static final String SAMPLE_JWK_RSA_SIG = "   {\n" +
            "     \"kty\": \"RSA\",\n" +
            "     \"kid\": \"bilbo.baggins@hobbiton.example\",\n" +
            "     \"use\": \"sig\",\n" +
            "     \"n\": \"n4EPtAOCc9AlkeQHPzHStgAbgs7bTZLwUBZdR8_KuKPEHLd4rHVTeT\n" +
            "         -O-XV2jRojdNhxJWTDvNd7nqQ0VEiZQHz_AJmSCpMaJMRBSFKrKb2wqV\n" +
            "         wGU_NsYOYL-QtiWN2lbzcEe6XC0dApr5ydQLrHqkHHig3RBordaZ6Aj-\n" +
            "         oBHqFEHYpPe7Tpe-OfVfHd1E6cS6M1FZcD1NNLYD5lFHpPI9bTwJlsde\n" +
            "         3uhGqC0ZCuEHg8lhzwOHrtIQbS0FVbb9k3-tVTU4fg_3L_vniUFAKwuC\n" +
            "         LqKnS2BYwdq_mzSnbLY7h_qixoR7jig3__kRhuaxwUkRz5iaiQkqgc5g\n" +
            "         HdrNP5zw\",\n" +
            "     \"e\": \"AQAB\",\n" +
            "     \"d\": \"bWUC9B-EFRIo8kpGfh0ZuyGPvMNKvYWNtB_ikiH9k20eT-O1q_I78e\n" +
            "         iZkpXxXQ0UTEs2LsNRS-8uJbvQ-A1irkwMSMkK1J3XTGgdrhCku9gRld\n" +
            "         Y7sNA_AKZGh-Q661_42rINLRCe8W-nZ34ui_qOfkLnK9QWDDqpaIsA-b\n" +
            "         MwWWSDFu2MUBYwkHTMEzLYGqOe04noqeq1hExBTHBOBdkMXiuFhUq1BU\n" +
            "         6l-DqEiWxqg82sXt2h-LMnT3046AOYJoRioz75tSUQfGCshWTBnP5uDj\n" +
            "         d18kKhyv07lhfSJdrPdM5Plyl21hsFf4L_mHCuoFau7gdsPfHPxxjVOc\n" +
            "         OpBrQzwQ\",\n" +
            "     \"p\": \"3Slxg_DwTXJcb6095RoXygQCAZ5RnAvZlno1yhHtnUex_fp7AZ_9nR\n" +
            "         aO7HX_-SFfGQeutao2TDjDAWU4Vupk8rw9JR0AzZ0N2fvuIAmr_WCsmG\n" +
            "         peNqQnev1T7IyEsnh8UMt-n5CafhkikzhEsrmndH6LxOrvRJlsPp6Zv8\n" +
            "         bUq0k\",\n" +
            "     \"q\": \"uKE2dh-cTf6ERF4k4e_jy78GfPYUIaUyoSSJuBzp3Cubk3OCqs6grT\n" +
            "         8bR_cu0Dm1MZwWmtdqDyI95HrUeq3MP15vMMON8lHTeZu2lmKvwqW7an\n" +
            "         V5UzhM1iZ7z4yMkuUwFWoBvyY898EXvRD-hdqRxHlSqAZ192zB3pVFJ0\n" +
            "         s7pFc\",\n" +
            "     \"dp\": \"B8PVvXkvJrj2L-GYQ7v3y9r6Kw5g9SahXBwsWUzp19TVlgI-YV85q\n" +
            "         1NIb1rxQtD-IsXXR3-TanevuRPRt5OBOdiMGQp8pbt26gljYfKU_E9xn\n" +
            "         -RULHz0-ed9E9gXLKD4VGngpz-PfQ_q29pk5xWHoJp009Qf1HvChixRX\n" +
            "         59ehik\",\n" +
            "     \"dq\": \"CLDmDGduhylc9o7r84rEUVn7pzQ6PF83Y-iBZx5NT-TpnOZKF1pEr\n" +
            "         AMVeKzFEl41DlHHqqBLSM0W1sOFbwTxYWZDm6sI6og5iTbwQGIC3gnJK\n" +
            "         bi_7k_vJgGHwHxgPaX2PnvP-zyEkDERuf-ry4c_Z11Cq9AqC2yeL6kdK\n" +
            "         T1cYF8\",\n" +
            "     \"qi\": \"3PiqvXQN0zwMeE-sBvZgi289XP9XCQF3VWqPzMKnIgQp7_Tugo6-N\n" +
            "         ZBKCQsMf3HaEGBjTVJs_jcK8-TRXvaKe-7ZMaQj8VfBdYkssbu0NKDDh\n" +
            "         jJ-GtiseaDVWt7dcH0cfwxgFUHpQh7FoCrjFJ6h6ZEpMF6xmujs4qMpP\n" +
            "         z8aaI4\"\n" +
            "   }";

    public static final String SAMPLE_JWK_RSA_ENC = "   {\n" +
            "     \"kty\": \"RSA\",\n" +
            "     \"kid\": \"bilbo.baggins@hobbiton.example\",\n" +
            "     \"use\": \"enc\",\n" +
            "     \"n\": \"n4EPtAOCc9AlkeQHPzHStgAbgs7bTZLwUBZdR8_KuKPEHLd4rHVTeT\n" +
            "         -O-XV2jRojdNhxJWTDvNd7nqQ0VEiZQHz_AJmSCpMaJMRBSFKrKb2wqV\n" +
            "         wGU_NsYOYL-QtiWN2lbzcEe6XC0dApr5ydQLrHqkHHig3RBordaZ6Aj-\n" +
            "         oBHqFEHYpPe7Tpe-OfVfHd1E6cS6M1FZcD1NNLYD5lFHpPI9bTwJlsde\n" +
            "         3uhGqC0ZCuEHg8lhzwOHrtIQbS0FVbb9k3-tVTU4fg_3L_vniUFAKwuC\n" +
            "         LqKnS2BYwdq_mzSnbLY7h_qixoR7jig3__kRhuaxwUkRz5iaiQkqgc5g\n" +
            "         HdrNP5zw\",\n" +
            "     \"e\": \"AQAB\",\n" +
            "     \"d\": \"bWUC9B-EFRIo8kpGfh0ZuyGPvMNKvYWNtB_ikiH9k20eT-O1q_I78e\n" +
            "         iZkpXxXQ0UTEs2LsNRS-8uJbvQ-A1irkwMSMkK1J3XTGgdrhCku9gRld\n" +
            "         Y7sNA_AKZGh-Q661_42rINLRCe8W-nZ34ui_qOfkLnK9QWDDqpaIsA-b\n" +
            "         MwWWSDFu2MUBYwkHTMEzLYGqOe04noqeq1hExBTHBOBdkMXiuFhUq1BU\n" +
            "         6l-DqEiWxqg82sXt2h-LMnT3046AOYJoRioz75tSUQfGCshWTBnP5uDj\n" +
            "         d18kKhyv07lhfSJdrPdM5Plyl21hsFf4L_mHCuoFau7gdsPfHPxxjVOc\n" +
            "         OpBrQzwQ\",\n" +
            "     \"p\": \"3Slxg_DwTXJcb6095RoXygQCAZ5RnAvZlno1yhHtnUex_fp7AZ_9nR\n" +
            "         aO7HX_-SFfGQeutao2TDjDAWU4Vupk8rw9JR0AzZ0N2fvuIAmr_WCsmG\n" +
            "         peNqQnev1T7IyEsnh8UMt-n5CafhkikzhEsrmndH6LxOrvRJlsPp6Zv8\n" +
            "         bUq0k\",\n" +
            "     \"q\": \"uKE2dh-cTf6ERF4k4e_jy78GfPYUIaUyoSSJuBzp3Cubk3OCqs6grT\n" +
            "         8bR_cu0Dm1MZwWmtdqDyI95HrUeq3MP15vMMON8lHTeZu2lmKvwqW7an\n" +
            "         V5UzhM1iZ7z4yMkuUwFWoBvyY898EXvRD-hdqRxHlSqAZ192zB3pVFJ0\n" +
            "         s7pFc\",\n" +
            "     \"dp\": \"B8PVvXkvJrj2L-GYQ7v3y9r6Kw5g9SahXBwsWUzp19TVlgI-YV85q\n" +
            "         1NIb1rxQtD-IsXXR3-TanevuRPRt5OBOdiMGQp8pbt26gljYfKU_E9xn\n" +
            "         -RULHz0-ed9E9gXLKD4VGngpz-PfQ_q29pk5xWHoJp009Qf1HvChixRX\n" +
            "         59ehik\",\n" +
            "     \"dq\": \"CLDmDGduhylc9o7r84rEUVn7pzQ6PF83Y-iBZx5NT-TpnOZKF1pEr\n" +
            "         AMVeKzFEl41DlHHqqBLSM0W1sOFbwTxYWZDm6sI6og5iTbwQGIC3gnJK\n" +
            "         bi_7k_vJgGHwHxgPaX2PnvP-zyEkDERuf-ry4c_Z11Cq9AqC2yeL6kdK\n" +
            "         T1cYF8\",\n" +
            "     \"qi\": \"3PiqvXQN0zwMeE-sBvZgi289XP9XCQF3VWqPzMKnIgQp7_Tugo6-N\n" +
            "         ZBKCQsMf3HaEGBjTVJs_jcK8-TRXvaKe-7ZMaQj8VfBdYkssbu0NKDDh\n" +
            "         jJ-GtiseaDVWt7dcH0cfwxgFUHpQh7FoCrjFJ6h6ZEpMF6xmujs4qMpP\n" +
            "         z8aaI4\"\n" +
            "   }";

    public static final String SAMPLE_JWKS = "   {\n" +
            "     \"keys\": [\n" +
            "       {\n" +
            "         \"kty\": \"oct\",\n" +
            "         \"kid\": \"77c7e2b8-6e13-45cf-8672-617b5b45243a\",\n" +
            "         \"use\": \"enc\",\n" +
            "         \"alg\": \"A128GCM\",\n" +
            "         \"k\": \"XctOhJAkA-pD9Lh7ZgW_2A\"\n" +
            "       },\n" +
            "       {\n" +
            "         \"kty\": \"oct\",\n" +
            "         \"kid\": \"81b20965-8332-43d9-a468-82160ad91ac8\",\n" +
            "         \"use\": \"enc\",\n" +
            "         \"alg\": \"A128KW\",\n" +
            "         \"k\": \"GZy6sIZ6wl9NJOKB-jnmVQ\"\n" +
            "       },\n" +
            "       {\n" +
            "         \"kty\": \"oct\",\n" +
            "         \"kid\": \"18ec08e1-bfa9-4d95-b205-2b4dd1d4321d\",\n" +
            "         \"use\": \"enc\",\n" +
            "         \"alg\": \"A256GCMKW\",\n" +
            "         \"k\": \"qC57l_uxcm7Nm3K-ct4GFjx8tM1U8CZ0NLBvdQstiS8\"\n" +
            "       },\n" +
            "       {\n" +
            "         \"kty\": \"RSA\",\n" +
            "         \"kid\": \"bilbo.baggins@hobbiton.example\",\n" +
            "         \"use\": \"sig\",\n" +
            "         \"n\": \"n4EPtAOCc9AlkeQHPzHStgAbgs7bTZLwUBZdR8_KuKPEHLd4rHVTeT\n" +
            "             -O-XV2jRojdNhxJWTDvNd7nqQ0VEiZQHz_AJmSCpMaJMRBSFKrKb2wqV\n" +
            "             wGU_NsYOYL-QtiWN2lbzcEe6XC0dApr5ydQLrHqkHHig3RBordaZ6Aj-\n" +
            "             oBHqFEHYpPe7Tpe-OfVfHd1E6cS6M1FZcD1NNLYD5lFHpPI9bTwJlsde\n" +
            "             3uhGqC0ZCuEHg8lhzwOHrtIQbS0FVbb9k3-tVTU4fg_3L_vniUFAKwuC\n" +
            "             LqKnS2BYwdq_mzSnbLY7h_qixoR7jig3__kRhuaxwUkRz5iaiQkqgc5g\n" +
            "             HdrNP5zw\",\n" +
            "         \"e\": \"AQAB\",\n" +
            "         \"d\": \"bWUC9B-EFRIo8kpGfh0ZuyGPvMNKvYWNtB_ikiH9k20eT-O1q_I78e\n" +
            "             iZkpXxXQ0UTEs2LsNRS-8uJbvQ-A1irkwMSMkK1J3XTGgdrhCku9gRld\n" +
            "             Y7sNA_AKZGh-Q661_42rINLRCe8W-nZ34ui_qOfkLnK9QWDDqpaIsA-b\n" +
            "             MwWWSDFu2MUBYwkHTMEzLYGqOe04noqeq1hExBTHBOBdkMXiuFhUq1BU\n" +
            "             6l-DqEiWxqg82sXt2h-LMnT3046AOYJoRioz75tSUQfGCshWTBnP5uDj\n" +
            "             d18kKhyv07lhfSJdrPdM5Plyl21hsFf4L_mHCuoFau7gdsPfHPxxjVOc\n" +
            "             OpBrQzwQ\",\n" +
            "         \"p\": \"3Slxg_DwTXJcb6095RoXygQCAZ5RnAvZlno1yhHtnUex_fp7AZ_9nR\n" +
            "             aO7HX_-SFfGQeutao2TDjDAWU4Vupk8rw9JR0AzZ0N2fvuIAmr_WCsmG\n" +
            "             peNqQnev1T7IyEsnh8UMt-n5CafhkikzhEsrmndH6LxOrvRJlsPp6Zv8\n" +
            "             bUq0k\",\n" +
            "         \"q\": \"uKE2dh-cTf6ERF4k4e_jy78GfPYUIaUyoSSJuBzp3Cubk3OCqs6grT\n" +
            "             8bR_cu0Dm1MZwWmtdqDyI95HrUeq3MP15vMMON8lHTeZu2lmKvwqW7an\n" +
            "             V5UzhM1iZ7z4yMkuUwFWoBvyY898EXvRD-hdqRxHlSqAZ192zB3pVFJ0\n" +
            "             s7pFc\",\n" +
            "         \"dp\": \"B8PVvXkvJrj2L-GYQ7v3y9r6Kw5g9SahXBwsWUzp19TVlgI-YV85q\n" +
            "             1NIb1rxQtD-IsXXR3-TanevuRPRt5OBOdiMGQp8pbt26gljYfKU_E9xn\n" +
            "             -RULHz0-ed9E9gXLKD4VGngpz-PfQ_q29pk5xWHoJp009Qf1HvChixRX\n" +
            "             59ehik\",\n" +
            "         \"dq\": \"CLDmDGduhylc9o7r84rEUVn7pzQ6PF83Y-iBZx5NT-TpnOZKF1pEr\n" +
            "             AMVeKzFEl41DlHHqqBLSM0W1sOFbwTxYWZDm6sI6og5iTbwQGIC3gnJK\n" +
            "             bi_7k_vJgGHwHxgPaX2PnvP-zyEkDERuf-ry4c_Z11Cq9AqC2yeL6kdK\n" +
            "             T1cYF8\",\n" +
            "         \"qi\": \"3PiqvXQN0zwMeE-sBvZgi289XP9XCQF3VWqPzMKnIgQp7_Tugo6-N\n" +
            "             ZBKCQsMf3HaEGBjTVJs_jcK8-TRXvaKe-7ZMaQj8VfBdYkssbu0NKDDh\n" +
            "             jJ-GtiseaDVWt7dcH0cfwxgFUHpQh7FoCrjFJ6h6ZEpMF6xmujs4qMpP\n" +
            "             z8aaI4\"\n" +
            "       },\n" +
            "       {\n" +
            "         \"kty\": \"RSA\",\n" +
            "         \"kid\": \"bilbo.baggins@hobbiton.example\",\n" +
            "         \"use\": \"enc\",\n" +
            "         \"n\": \"n4EPtAOCc9AlkeQHPzHStgAbgs7bTZLwUBZdR8_KuKPEHLd4rHVTeT\n" +
            "             -O-XV2jRojdNhxJWTDvNd7nqQ0VEiZQHz_AJmSCpMaJMRBSFKrKb2wqV\n" +
            "             wGU_NsYOYL-QtiWN2lbzcEe6XC0dApr5ydQLrHqkHHig3RBordaZ6Aj-\n" +
            "             oBHqFEHYpPe7Tpe-OfVfHd1E6cS6M1FZcD1NNLYD5lFHpPI9bTwJlsde\n" +
            "             3uhGqC0ZCuEHg8lhzwOHrtIQbS0FVbb9k3-tVTU4fg_3L_vniUFAKwuC\n" +
            "             LqKnS2BYwdq_mzSnbLY7h_qixoR7jig3__kRhuaxwUkRz5iaiQkqgc5g\n" +
            "             HdrNP5zw\",\n" +
            "         \"e\": \"AQAB\",\n" +
            "         \"d\": \"bWUC9B-EFRIo8kpGfh0ZuyGPvMNKvYWNtB_ikiH9k20eT-O1q_I78e\n" +
            "             iZkpXxXQ0UTEs2LsNRS-8uJbvQ-A1irkwMSMkK1J3XTGgdrhCku9gRld\n" +
            "             Y7sNA_AKZGh-Q661_42rINLRCe8W-nZ34ui_qOfkLnK9QWDDqpaIsA-b\n" +
            "             MwWWSDFu2MUBYwkHTMEzLYGqOe04noqeq1hExBTHBOBdkMXiuFhUq1BU\n" +
            "             6l-DqEiWxqg82sXt2h-LMnT3046AOYJoRioz75tSUQfGCshWTBnP5uDj\n" +
            "             d18kKhyv07lhfSJdrPdM5Plyl21hsFf4L_mHCuoFau7gdsPfHPxxjVOc\n" +
            "             OpBrQzwQ\",\n" +
            "         \"p\": \"3Slxg_DwTXJcb6095RoXygQCAZ5RnAvZlno1yhHtnUex_fp7AZ_9nR\n" +
            "             aO7HX_-SFfGQeutao2TDjDAWU4Vupk8rw9JR0AzZ0N2fvuIAmr_WCsmG\n" +
            "             peNqQnev1T7IyEsnh8UMt-n5CafhkikzhEsrmndH6LxOrvRJlsPp6Zv8\n" +
            "             bUq0k\",\n" +
            "         \"q\": \"uKE2dh-cTf6ERF4k4e_jy78GfPYUIaUyoSSJuBzp3Cubk3OCqs6grT\n" +
            "             8bR_cu0Dm1MZwWmtdqDyI95HrUeq3MP15vMMON8lHTeZu2lmKvwqW7an\n" +
            "             V5UzhM1iZ7z4yMkuUwFWoBvyY898EXvRD-hdqRxHlSqAZ192zB3pVFJ0\n" +
            "             s7pFc\",\n" +
            "         \"dp\": \"B8PVvXkvJrj2L-GYQ7v3y9r6Kw5g9SahXBwsWUzp19TVlgI-YV85q\n" +
            "             1NIb1rxQtD-IsXXR3-TanevuRPRt5OBOdiMGQp8pbt26gljYfKU_E9xn\n" +
            "             -RULHz0-ed9E9gXLKD4VGngpz-PfQ_q29pk5xWHoJp009Qf1HvChixRX\n" +
            "             59ehik\",\n" +
            "         \"dq\": \"CLDmDGduhylc9o7r84rEUVn7pzQ6PF83Y-iBZx5NT-TpnOZKF1pEr\n" +
            "             AMVeKzFEl41DlHHqqBLSM0W1sOFbwTxYWZDm6sI6og5iTbwQGIC3gnJK\n" +
            "             bi_7k_vJgGHwHxgPaX2PnvP-zyEkDERuf-ry4c_Z11Cq9AqC2yeL6kdK\n" +
            "             T1cYF8\",\n" +
            "         \"qi\": \"3PiqvXQN0zwMeE-sBvZgi289XP9XCQF3VWqPzMKnIgQp7_Tugo6-N\n" +
            "             ZBKCQsMf3HaEGBjTVJs_jcK8-TRXvaKe-7ZMaQj8VfBdYkssbu0NKDDh\n" +
            "             jJ-GtiseaDVWt7dcH0cfwxgFUHpQh7FoCrjFJ6h6ZEpMF6xmujs4qMpP\n" +
            "             z8aaI4\"\n" +
            "       },       \n" +
            "       {\n" +
            "         \"kty\": \"EC\",\n" +
            "         \"kid\": \"bilbo.baggins@hobbiton.example\",\n" +
            "         \"use\": \"sig\",\n" +
            "         \"crv\": \"P-521\",\n" +
            "         \"x\": \"AHKZLLOsCOzz5cY97ewNUajB957y-C-U88c3v13nmGZx6sYl_oJXu9\n" +
            "             A5RkTKqjqvjyekWF-7ytDyRXYgCF5cj0Kt\",\n" +
            "         \"y\": \"AdymlHvOiLxXkEhayXQnNCvDX4h9htZaCJN34kfmC6pV5OhQHiraVy\n" +
            "             SsUdaQkAgDPrwQrJmbnX9cwlGfP-HqHZR1\",\n" +
            "         \"d\": \"AAhRON2r9cqXX1hg-RoI6R1tX5p2rUAYdmpHZoC1XNM56KtscrX6zb\n" +
            "             KipQrCW9CGZH3T4ubpnoTKLDYJ_fF3_rJt\"\n" +
            "       },\n" +
            "       {\n" +
            "         \"kty\": \"oct\",\n" +
            "         \"kid\": \"018c0ae5-4d9b-471b-bfd6-eef314bc7037\",\n" +
            "         \"use\": \"sig\",\n" +
            "         \"alg\": \"HS256\",\n" +
            "         \"k\": \"hJtXIZ2uSN5kbQfbtTNWbpdmhkV8FJG-Onbc6mxCcYg\"\n" +
            "       }       \n" +
            "     ]\n" +
            "   }";
}
