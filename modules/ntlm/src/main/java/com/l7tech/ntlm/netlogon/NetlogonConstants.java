package com.l7tech.ntlm.netlogon;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 3/13/12
 */
public final class NetlogonConstants {
    public enum NetlogonLogonInfoClass{
        NetlogonInteractiveInformation(1),
        NetlogonNetworkInformation(2),
        NetlogonServiceInformation(3),
        NetlogonGenericInformation(4),
        NetlogonInteractiveTransitiveInformation(5),
        NetlogonNetworkTransitiveInformation(6),
        NetlogonServiceTransitiveInformation(7);

        public short value;

        private NetlogonLogonInfoClass(int val) {
            this.value = (short)val;
        }
    }

    public enum NetlogonValidationInfoClass {
        NetlogonValidationUasInfo(1),
        NetlogonValidationSamInfo(2),
        NetlogonValidationSamInfo2(3),
        NetlogonValidationGenericInfo(4),
        NetlogonValidationGenericInfo2(5),
        NetlogonValidationSamInfo4(6);
        
        public short value;
        
        private NetlogonValidationInfoClass(int val) {
            this.value = (short)val;
        }
    }

    public enum NetlogonSecureChannelType{
        NullSecureChannel(0),
        MsvApSecureChannel(1),
        WorkstationSecureChannel(2),
        TrustedDnsDomainSecureChannel(3),
        TrustedDomainSecureChannel(4),
        UasServerSecureChannel(5),
        ServerSecureChannel(6),
        CdcServerSecureChannel(7);

        public short value;

        private NetlogonSecureChannelType(int val) {
            this.value = (short)val;
        }
    }
}
