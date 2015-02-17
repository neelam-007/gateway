package com.l7tech.kerberos.delegate;

import sun.security.krb5.*;
import sun.security.krb5.internal.*;
import sun.security.krb5.internal.crypto.KeyUsage;
import sun.security.util.*;

import java.io.IOException;

public class DelegateKrbTgsRep {

    private Credentials creds;

    public DelegateKrbTgsRep(byte[] ibuf, DelegateKrbTgsReq tgsReq)
            throws KrbException, IOException {
        DerValue derValue = new DerValue(ibuf);
        TGSRep rep;
        try {
            rep = new TGSRep(derValue);
        } catch (Asn1Exception e) {
            KrbException ke = new KrbException(new KRBError(derValue));
            ke.initCause(e);
            throw ke;
        }
        EncTGSRepPart repPart = decrypt(rep, tgsReq.getTgsReqKey());

/*  TODO JDK 8 fixes -- this code doesn't compile due to removed method and fields.
    TODO check whether commenting this out might break kerberos delegation feature

        if (rep.ticket.sname.getRealm() == null) {
            rep.ticket.sname.setRealm(rep.ticket.realm);
        }

        if (rep.cname.getRealm() == null) {
            rep.cname.setRealm(rep.crealm);
        }
*/

        this.creds = new Credentials(rep.ticket,
                rep.cname,
                rep.ticket.sname,
                repPart.key,
                repPart.flags,
                repPart.authtime,
                repPart.starttime,
                repPart.endtime,
                repPart.renewTill,
                repPart.caddr
        );
    }

    public Credentials getCreds() {
        return creds;
    }
    
    private EncTGSRepPart decrypt(TGSRep rep, EncryptionKey key) throws KrbException, IOException {
        byte[] decryptData = rep.encPart.decrypt(key,
                KeyUsage.KU_ENC_TGS_REP_PART_SESSKEY);

        byte[] resetData = rep.encPart.reset(decryptData);
        DerValue ref = new DerValue(resetData);
        return new EncTGSRepPart(ref) {
            @Override
            protected void init(DerValue encoding, int rep_type) throws Asn1Exception, IOException, RealmException {
                try {
                    super.init(encoding, rep_type);
                } catch (Asn1Exception e) {
                    //For multiple referral tgt, extra data is returned,
                    //exception thrown when der.getData().available()
                    //For now just ignore the data.
                }
            }
        };
    }
}
