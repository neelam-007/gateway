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
 * Date: 3/12/12
 */
public class NetlogonValidationSamInfo2 extends NdrObject{
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
    //-------------------------------------
    //not in the original structure spec
     public LmOwfPassword lmOwfPassword;
     public int accountFlags;
    //-------------------------------------
     public int[] expansionRoom;
     public int sidCount;
     public NetlogonSidAndAttributes[] extraSids;
    
    @Override
    public void encode(NdrBuffer dst) throws NdrException {
       dst.align(8);
       dst.enc_ndr_hyper(this.logonTime);
       dst.enc_ndr_hyper(this.logoffTime);
       dst.enc_ndr_hyper(this.kickOffTime);
       dst.enc_ndr_hyper(this.passwordLastSet);
       dst.enc_ndr_hyper(this.passwordCanChange);
       dst.enc_ndr_hyper(this.passwordMustChange);
       dst.enc_ndr_short(this.effectiveName.length);
       dst.enc_ndr_short(this.effectiveName.maximum_length);
       dst.enc_ndr_referent(this.effectiveName.buffer, 1);
       dst.enc_ndr_short(this.fullName.length);
       dst.enc_ndr_short(this.fullName.maximum_length);
       dst.enc_ndr_referent(this.fullName.buffer, 1);
       dst.enc_ndr_short(this.logonScript.length);
       dst.enc_ndr_short(this.logonScript.maximum_length);
       dst.enc_ndr_referent(this.logonScript.buffer, 1);
       dst.enc_ndr_short(this.profilePath.length);
       dst.enc_ndr_short(this.profilePath.maximum_length);
       dst.enc_ndr_referent(this.profilePath.buffer, 1);
       dst.enc_ndr_short(this.homeDirectory.length);
       dst.enc_ndr_short(this.homeDirectory.maximum_length);
       dst.enc_ndr_referent(this.homeDirectory.buffer, 1);
       dst.enc_ndr_short(this.homeDirectoryDrive.length);
       dst.enc_ndr_short(this.homeDirectoryDrive.maximum_length);
       dst.enc_ndr_referent(this.homeDirectoryDrive.buffer, 1);
       dst.enc_ndr_short(this.logonCount);
       dst.enc_ndr_short(this.badPasswordCount);
       dst.enc_ndr_long(this.userId);
       dst.enc_ndr_long(this.primaryGroupId);
       dst.enc_ndr_long(this.groups.count);
       dst.enc_ndr_referent(this.groups.rids, 1);
       dst.enc_ndr_long(this.userFlags);
       int userSessionKeySize = 16;
       int userSessionKeyIndex = dst.index;
       dst.advance(1 * userSessionKeySize);
       dst.enc_ndr_short(this.logonServer.length);
       dst.enc_ndr_short(this.logonServer.maximum_length);
       dst.enc_ndr_referent(this.logonServer.buffer, 1);
       dst.enc_ndr_short(this.logonDomainName.length);
       dst.enc_ndr_short(this.logonDomainName.maximum_length);
       dst.enc_ndr_referent(this.logonDomainName.buffer, 1);
       dst.enc_ndr_referent(this.logonDomainSid, 1);
       int lmSessionKeySize = 8;
       int lmSessionKeyIndex = dst.index;
       dst.advance(1 * lmSessionKeySize);
        dst.enc_ndr_long(this.accountFlags);
       int expansionRoomSize = 7;
       int expansionRoomIndex = dst.index;
       dst.advance(4 * expansionRoomSize);
       dst.enc_ndr_long(this.sidCount);
       dst.enc_ndr_referent(this.extraSids, 1);

       dst =  DceRpcUtil.encode_rpc_string_buffer(dst, effectiveName);

       dst = DceRpcUtil.encode_rpc_string_buffer(dst, fullName);

       dst = DceRpcUtil.encode_rpc_string_buffer(dst, logonScript);

       dst = DceRpcUtil.encode_rpc_string_buffer(dst, profilePath);

       dst = DceRpcUtil.encode_rpc_string_buffer(dst, homeDirectory);

       dst = DceRpcUtil.encode_rpc_string_buffer(dst, homeDirectoryDrive);

       if (this.groups.rids != null) {
         dst = dst.deferred;
         int size = this.groups.count;
         dst.enc_ndr_long(size);
         int index = dst.index;
         dst.advance(8 * size);

         dst = dst.derive(index);
         for (int i = 0; i < size; i++) {
           this.groups.rids[i].encode(dst);
         }
       }
       dst = dst.derive(userSessionKeyIndex);
       for (int i = 0; i < userSessionKeySize; i++) {
         dst.enc_ndr_small(this.userSesionKey.key[i]);
       }

       dst = DceRpcUtil.encode_rpc_string_buffer(dst, logonServer);

       dst = DceRpcUtil.encode_rpc_string_buffer(dst, logonDomainName);

       if (this.logonDomainSid != null) {
         dst = dst.deferred;
         this.logonDomainSid.encode(dst);
       }

       dst = dst.derive(lmSessionKeyIndex);
       for (int i = 0; i < lmSessionKeySize; i++) {
         dst.enc_ndr_small(this.lmOwfPassword.key[i]);
       }
       dst = dst.derive(expansionRoomIndex);
       for (int i = 0; i < expansionRoomSize; i++) {
         dst.enc_ndr_long(this.expansionRoom[i]);
       }
       if (this.extraSids != null) {
         dst = dst.deferred;
         int size = this.sidCount;
         dst.enc_ndr_long(size);
         int index = dst.index;
         dst.advance(8 * size);

         dst = dst.derive(index);
         for (int i = 0; i < size; i++)
           this.extraSids[i].encode(dst);
       }
    }

    @Override
    public void decode(NdrBuffer src) throws NdrException {
       src.align(8);
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
       this.effectiveName.length = (short)src.dec_ndr_short();
       this.effectiveName.maximum_length = (short)src.dec_ndr_short();
       int effectiveNameBufferRef = src.dec_ndr_long();
       src.align(4);
       if (this.fullName == null) {
         this.fullName = new rpc.unicode_string();
       }
       this.fullName.length = (short)src.dec_ndr_short();
       this.fullName.maximum_length = (short)src.dec_ndr_short();
       int fullNameBufferRef = src.dec_ndr_long();
       src.align(4);
       if (this.logonScript == null) {
         this.logonScript = new rpc.unicode_string();
       }
       this.logonScript.length = (short)src.dec_ndr_short();
       this.logonScript.maximum_length = (short)src.dec_ndr_short();
       int logonScriptBufferRef = src.dec_ndr_long();
       src.align(4);
       if (this.profilePath == null) {
         this.profilePath = new rpc.unicode_string();
       }
       this.profilePath.length = (short)src.dec_ndr_short();
       this.profilePath.maximum_length = (short)src.dec_ndr_short();
       int profilePathBufferRef = src.dec_ndr_long();
       src.align(4);
       if (this.homeDirectory == null) {
         this.homeDirectory = new rpc.unicode_string();
       }
       this.homeDirectory.length = (short)src.dec_ndr_short();
       this.homeDirectory.maximum_length = (short)src.dec_ndr_short();
       int homeDirectoryBufferRef = src.dec_ndr_long();
       src.align(4);
       if (this.homeDirectoryDrive == null) {
         this.homeDirectoryDrive = new rpc.unicode_string();
       }
       this.homeDirectoryDrive.length = (short)src.dec_ndr_short();
       this.homeDirectoryDrive.maximum_length = (short)src.dec_ndr_short();
       int homeDirectoryDriveBufferRef = src.dec_ndr_long();
       this.logonCount = (short)src.dec_ndr_short();
       this.badPasswordCount = (short)src.dec_ndr_short();
       this.userId = src.dec_ndr_long();
       this.primaryGroupId = src.dec_ndr_long();
       src.align(4);
       if (this.groups == null) {
         this.groups = new samr.SamrRidWithAttributeArray();
       }
       this.groups.count = src.dec_ndr_long();
       int groupsRidRef = src.dec_ndr_long();
       this.userFlags = src.dec_ndr_long();
       src.align(1);
       if (this.userSesionKey == null) {
         this.userSesionKey = new UserSessionKey();
       }
       int userSessionKeySize = 16;
       int userSessionKeyIndex = src.index;
       src.advance(1 * userSessionKeySize);
       src.align(4);
       if (this.logonServer == null) {
         this.logonServer = new rpc.unicode_string();
       }
       this.logonServer.length = (short)src.dec_ndr_short();
       this.logonServer.maximum_length = (short)src.dec_ndr_short();
       int logonServerBufferRef = src.dec_ndr_long();
       src.align(4);
       if (this.logonDomainName == null) {
         this.logonDomainName = new rpc.unicode_string();
       }
       this.logonDomainName.length = (short)src.dec_ndr_short();
       this.logonDomainName.maximum_length = (short)src.dec_ndr_short();
       int logonDomainNameBufferRef = src.dec_ndr_long();
       int logonDomainNameRef = src.dec_ndr_long();
       src.align(1);
       if (this.lmOwfPassword == null) {
         this.lmOwfPassword = new LmOwfPassword();
       }
       int lmSessionKeySize = 8;
       int lmSessionKeyIndex = src.index;
       src.advance(1 * lmSessionKeySize);
       this.accountFlags = src.dec_ndr_long();
       int expansionRoomSize = 7;
       int expansionRoomIndex = src.index;
       src.advance(4 * expansionRoomSize);
       this.sidCount = src.dec_ndr_long();
       int extraSidsRef = src.dec_ndr_long();

       if (effectiveNameBufferRef != 0) {
         src = DceRpcUtil.decode_rpc_string(src, effectiveName);
       }
       if (fullNameBufferRef != 0) {
         src = DceRpcUtil.decode_rpc_string(src, fullName);
       }
       if (logonScriptBufferRef != 0) {
         src = DceRpcUtil.decode_rpc_string(src, logonScript);
       }
       if (profilePathBufferRef != 0) {
         src = DceRpcUtil.decode_rpc_string(src, this.profilePath);
       }
       if (homeDirectoryBufferRef != 0) {
         src = DceRpcUtil.decode_rpc_string(src, this.homeDirectory);
       }
       if (homeDirectoryDriveBufferRef != 0) {
         src = DceRpcUtil.decode_rpc_string(src, this.homeDirectoryDrive);
       }
       if (groupsRidRef != 0) {
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
         this.userSesionKey.key[i] = (byte)src.dec_ndr_small();
       }
       if (logonServerBufferRef != 0) {
         src = DceRpcUtil.decode_rpc_string(src, this.logonServer);
       }
       if (logonDomainNameBufferRef != 0) {
         src = src.deferred;
         int logonDomainNameBufferSize = src.dec_ndr_long();
         src.dec_ndr_long();
         int logonDomainNameBufferLength = src.dec_ndr_long();
         int logonDomainNameBufferIndex = src.index;
         src.advance(2 * logonDomainNameBufferLength);

         if (this.logonDomainName.buffer == null) {
           if ((logonDomainNameBufferSize < 0) || (logonDomainNameBufferSize > 65535)) throw new NdrException("invalid buffer size");
           this.logonDomainName.buffer = new short[logonDomainNameBufferSize];
         }
         src = src.derive(logonDomainNameBufferIndex);
         for (int i = 0; i < logonDomainNameBufferLength; i++) {
           this.logonDomainName.buffer[i] = (short)src.dec_ndr_short();
         }
       }
       if (logonDomainNameRef != 0) {
         if (this.logonDomainSid == null) {
           this.logonDomainSid = new rpc.sid_t();

            }
         src = src.deferred;
         this.logonDomainSid.decode(src);
       }

       if (this.lmOwfPassword.key == null) {
         if ((lmSessionKeySize < 0) || (lmSessionKeySize > 65535)) throw new NdrException("invalid array size");
         this.lmOwfPassword.key = new byte[lmSessionKeySize];
       }
       src = src.derive(lmSessionKeyIndex);
       for (int i = 0; i < lmSessionKeySize; i++) {
         this.lmOwfPassword.key[i] = (byte)src.dec_ndr_small();
       }
       if (this.expansionRoom == null) {
         if ((expansionRoomSize < 0) || (expansionRoomSize > 65535)) throw new NdrException("invalid array size");
         this.expansionRoom = new int[expansionRoomSize];
       }
       src = src.derive(expansionRoomIndex);
       for (int i = 0; i < expansionRoomSize; i++) {
         this.expansionRoom[i] = src.dec_ndr_long();
       }
       if (extraSidsRef != 0) {
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

}
