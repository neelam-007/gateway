package com.l7tech.ntlm.netlogon;

import com.l7tech.ntlm.util.DceRpcUtil;
import jcifs.dcerpc.msrpc.samr;
import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;
import jcifs.dcerpc.ndr.NdrObject;
import jcifs.dcerpc.rpc;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public class NetlogonValidationSamInfo2 extends NdrObject {
    public long logonTime;
    public long logoffTime;
    public long kickOffTime;
    public long passwordLastSet;
    public long passwordCanChange;
    public long passwordMustChange;
    public rpc.unicode_string effectiveName;
    public rpc.unicode_string fullName;
    public rpc.unicode_string logonScript;
    public rpc.unicode_string profilePath;
    public rpc.unicode_string homeDirectory;
    public rpc.unicode_string homeDirectoryDrive;
    public short logonCount;
    public short badPasswordCount;
    public int userId;
    public int primaryGroupId;
    public samr.SamrRidWithAttributeArray groups;
    public int userFlags;
    public UserSessionKey userSesionKey;
    public rpc.unicode_string logonServer;
    public rpc.unicode_string logonDomainName;
    public rpc.sid_t logonDomainSid;

    public int[] expansionRoom;
    public int sidCount;
    public NetlogonSidAndAttributes[] extraSids;

    @Override
    public void decode(NdrBuffer src) throws NdrException {
        this.logonTime = src.dec_ndr_hyper();
        this.logoffTime = src.dec_ndr_hyper();
        this.kickOffTime = src.dec_ndr_hyper();
        this.passwordLastSet = src.dec_ndr_hyper();
        this.passwordCanChange = src.dec_ndr_hyper();
        this.passwordMustChange = src.dec_ndr_hyper();
        src.align(4);
        if (this.effectiveName == null) {
            this.effectiveName = new rpc.unicode_string();
        }
        this.effectiveName.length = (short) src.dec_ndr_short();
        this.effectiveName.maximum_length = (short) src.dec_ndr_short();
        int effectiveNameBufferPtr = src.dec_ndr_long();
        src.align(4);
        if (this.fullName == null) {
            this.fullName = new rpc.unicode_string();
        }
        this.fullName.length = (short) src.dec_ndr_short();
        this.fullName.maximum_length = (short) src.dec_ndr_short();
        int fullNameBufferPtr = src.dec_ndr_long();
        src.align(4);
        if (this.logonScript == null) {
            this.logonScript = new rpc.unicode_string();
        }
        this.logonScript.length = (short) src.dec_ndr_short();
        this.logonScript.maximum_length = (short) src.dec_ndr_short();
        int logonScriptBufferPtr = src.dec_ndr_long();
        src.align(4);
        if (this.profilePath == null) {
            this.profilePath = new rpc.unicode_string();
        }
        this.profilePath.length = (short) src.dec_ndr_short();
        this.profilePath.maximum_length = (short) src.dec_ndr_short();
        int profilePathBufferPtr = src.dec_ndr_long();
        src.align(4);
        if (this.homeDirectory == null) {
            this.homeDirectory = new rpc.unicode_string();
        }
        this.homeDirectory.length = (short) src.dec_ndr_short();
        this.homeDirectory.maximum_length = (short) src.dec_ndr_short();
        int homeDirectoryBufferPtr = src.dec_ndr_long();
        src.align(4);
        if (this.homeDirectoryDrive == null) {
            this.homeDirectoryDrive = new rpc.unicode_string();
        }
        this.homeDirectoryDrive.length = (short) src.dec_ndr_short();
        this.homeDirectoryDrive.maximum_length = (short) src.dec_ndr_short();
        int homeDirectoryDriveBufferPtr = src.dec_ndr_long();
        this.logonCount = (short) src.dec_ndr_short();
        this.badPasswordCount = (short) src.dec_ndr_short();
        this.userId = src.dec_ndr_long();
        this.primaryGroupId = src.dec_ndr_long();
        src.align(4);
        if (this.groups == null) {
            this.groups = new samr.SamrRidWithAttributeArray();
        }
        this.groups.count = src.dec_ndr_long();
        int groupsRidPtr = src.dec_ndr_long();
        this.userFlags = src.dec_ndr_long();
        src.align(1);
        if (this.userSesionKey == null) {
            this.userSesionKey = new UserSessionKey();
        }
        int userSessionKeySize = 16;
        int userSessionKeyIndex = src.index;
        src.advance(userSessionKeySize);
        src.align(4);
        if (this.logonServer == null) {
            this.logonServer = new rpc.unicode_string();
        }
        this.logonServer.length = (short) src.dec_ndr_short();
        this.logonServer.maximum_length = (short) src.dec_ndr_short();
        int logonServerBufferPtr = src.dec_ndr_long();
        src.align(4);
        if (this.logonDomainName == null) {
            this.logonDomainName = new rpc.unicode_string();
        }
        this.logonDomainName.length = (short) src.dec_ndr_short();
        this.logonDomainName.maximum_length = (short) src.dec_ndr_short();
        int logonDomainNameBufferPtr = src.dec_ndr_long();
        int logonDomainNamePtr = src.dec_ndr_long();
        src.align(1);
        int expansionRoomSize = 10;
        int expansionRoomIndex = src.index;
        src.advance(4 * expansionRoomSize);
        this.sidCount = src.dec_ndr_long();
        int extraSidsPtr = src.dec_ndr_long();

        if (effectiveNameBufferPtr != 0) {
            src = DceRpcUtil.decode_rpc_string(src, effectiveName);
        }
        if (fullNameBufferPtr != 0) {
            src = DceRpcUtil.decode_rpc_string(src, fullName);
        }
        if (logonScriptBufferPtr != 0) {
            src = DceRpcUtil.decode_rpc_string(src, logonScript);
        }
        if (profilePathBufferPtr != 0) {
            src = DceRpcUtil.decode_rpc_string(src, this.profilePath);
        }
        if (homeDirectoryBufferPtr != 0) {
            src = DceRpcUtil.decode_rpc_string(src, this.homeDirectory);
        }
        if (homeDirectoryDriveBufferPtr != 0) {
            src = DceRpcUtil.decode_rpc_string(src, this.homeDirectoryDrive);
        }
        if (groupsRidPtr != 0) {
            src = src.deferred;
            int groupsRidsSize = src.dec_ndr_long();
            int groupsRidsIndex = src.index;
            src.advance(8 * groupsRidsSize);

            if (this.groups.rids == null) {
                if ((groupsRidsSize < 0) || (groupsRidsSize > 65535)) throw new NdrException("invalid array size");
                this.groups.rids = new samr.SamrRidWithAttribute[groupsRidsSize];
            }
            src = src.derive(groupsRidsIndex);
            for (int i = 0; i < groupsRidsSize; i++) {
                if (this.groups.rids[i] == null) {
                    this.groups.rids[i] = new samr.SamrRidWithAttribute();
                }
                this.groups.rids[i].decode(src);
            }
        }
        if (this.userSesionKey.key == null) {
            if ((userSessionKeySize < 0) || (userSessionKeySize > 65535)) throw new NdrException("invalid array size");
            this.userSesionKey.key = new byte[userSessionKeySize];
        }
        src = src.derive(userSessionKeyIndex);
        for (int i = 0; i < userSessionKeySize; i++) {
            this.userSesionKey.key[i] = (byte) src.dec_ndr_small();
        }
        if (logonServerBufferPtr != 0) {
            src = DceRpcUtil.decode_rpc_string(src, this.logonServer);
        }
        if (logonDomainNameBufferPtr != 0) {
            src = src.deferred;
            int logonDomainNameBufferSize = src.dec_ndr_long();
            src.dec_ndr_long();
            int logonDomainNameBufferLength = src.dec_ndr_long();
            int logonDomainNameBufferIndex = src.index;
            src.advance(2 * logonDomainNameBufferLength);

            if (this.logonDomainName.buffer == null) {
                if ((logonDomainNameBufferSize < 0) || (logonDomainNameBufferSize > 65535))
                    throw new NdrException("invalid buffer size");
                this.logonDomainName.buffer = new short[logonDomainNameBufferSize];
            }
            src = src.derive(logonDomainNameBufferIndex);
            for (int i = 0; i < logonDomainNameBufferLength; i++) {
                this.logonDomainName.buffer[i] = (short) src.dec_ndr_short();
            }
        }
        if (logonDomainNamePtr != 0) {
            if (this.logonDomainSid == null) {
                this.logonDomainSid = new rpc.sid_t();

            }
            src = src.deferred;
            this.logonDomainSid.decode(src);
        }

        if (this.expansionRoom == null) {
            if ((expansionRoomSize < 0) || (expansionRoomSize > 65535)) throw new NdrException("invalid array size");
            this.expansionRoom = new int[expansionRoomSize];
        }
        src = src.derive(expansionRoomIndex);
        for (int i = 0; i < expansionRoomSize; i++) {
            this.expansionRoom[i] = src.dec_ndr_long();
        }
        if (extraSidsPtr != 0) {
            src = src.deferred;
            int extraSidsSize = src.dec_ndr_long();
            int extraSidsIndex = src.index;
            src.advance(8 * extraSidsSize);

            if (this.extraSids == null) {
                if ((extraSidsSize < 0) || (extraSidsSize > 65535)) throw new NdrException("invalid array size");
                this.extraSids = new NetlogonSidAndAttributes[extraSidsSize];
            }
            src = src.derive(extraSidsIndex);
            for (int i = 0; i < extraSidsSize; i++) {
                if (this.extraSids[i] == null) {
                    this.extraSids[i] = new NetlogonSidAndAttributes();
                }
                this.extraSids[i].decode(src);
            }
        }
    }

    @Override
    public void encode(NdrBuffer dst) throws NdrException {
        //no need to decode. Leave it empty
    }

}
