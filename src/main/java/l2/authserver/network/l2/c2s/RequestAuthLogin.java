package l2.authserver.network.l2.c2s;

import l2.authserver.Config;
import l2.authserver.IpBanManager;
import l2.authserver.accounts.Account;
import l2.authserver.accounts.SessionManager;
import l2.authserver.accounts.SessionManager.Session;
import l2.authserver.crypt.PasswordHash;
import l2.authserver.network.l2.L2LoginClient;
import l2.authserver.network.l2.L2LoginClient.LoginClientState;
import l2.authserver.network.l2.s2c.LoginFail.LoginFailReason;
import l2.authserver.network.l2.s2c.LoginOk;
import l2.authserver.utils.Log;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class RequestAuthLogin extends L2LoginClientPacket {
  private byte[] _raw = new byte[128];

  protected void readImpl() {
    this.readB(this._raw);
    this.readD();
    this.readD();
    this.readD();
    this.readD();
    this.readD();
    this.readD();
    this.readH();
    this.readC();
  }

  protected void runImpl() throws Exception {
    L2LoginClient client = this.getClient();

    byte[] decrypted;
    try {
      Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
      rsaCipher.init(2, client.getRSAPrivateKey());
      decrypted = rsaCipher.doFinal(this._raw, 0, 128);
    } catch (Exception e) {
      log.error("Exception: eMessage={}, eClass={}, eCause={}", e.getMessage(), this.getClass().getSimpleName(), e.getCause());
      client.closeNow(true);
      return;
    }

    String user = (new String(decrypted, 94, 14)).trim();
    user = user.toLowerCase();
    String password = (new String(decrypted, 108, 16)).trim();
    int ncotp = decrypted[124] & 255;
    ncotp |= (decrypted[125] & 255) << 8;
    ncotp |= (decrypted[126] & 255) << 16;
    int var10000 = ncotp | (decrypted[127] & 255) << 24;
    int currentTime = (int) (System.currentTimeMillis() / 1000L);
    Account account = new Account(user);
    account.restore();
    String passwordHash = Config.DEFAULT_CRYPT.encrypt(password);
    if (account.getPasswordHash() == null) {
      if (!Config.AUTO_CREATE_ACCOUNTS || !user.matches(Config.ANAME_TEMPLATE) || !password.matches(Config.APASSWD_TEMPLATE)) {
        client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
        return;
      }
      account.setPasswordHash(passwordHash);
      account.save();
    }

    boolean passwordCorrect = account.getPasswordHash().equalsIgnoreCase(passwordHash);
    if (!passwordCorrect) {
      PasswordHash[] var10 = Config.LEGACY_CRYPT;

      for (PasswordHash c : var10) {
        if (c.compare(password, account.getPasswordHash())) {
          passwordCorrect = true;
          account.setPasswordHash(passwordHash);
          break;
        }
      }
    }

    if (!IpBanManager.getInstance().tryLogin(client.getIpAddress(), passwordCorrect)) {
      client.closeNow(false);
    } else if (!passwordCorrect) {
      client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
    } else if (account.getAccessLevel() < 0) {
      client.close(LoginFailReason.REASON_ACCESS_FAILED);
    } else if (account.getBanExpire() > currentTime) {
      client.close(LoginFailReason.REASON_ACCESS_FAILED);
    } else if (!account.isAllowedIP(client.getIpAddress())) {
      client.close(LoginFailReason.REASON_ATTEMPTED_RESTRICTED_IP);
    } else {
      account.setLastAccess(currentTime);
      account.setLastIP(client.getIpAddress());
      Log.LogAccount(account);
      Session session = SessionManager.getInstance().openSession(account);
      client.setAuthed(true);
      client.setLogin(user);
      client.setAccount(account);
      client.setSessionKey(session.getSessionKey());
      client.setState(LoginClientState.AUTHED);
      client.sendPacket(new LoginOk(client.getSessionKey()));
    }
  }
}
