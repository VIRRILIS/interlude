package l2.authserver.network.l2.c2s;

import l2.authserver.Config;
import l2.authserver.IpBanManager;
import l2.authserver.accounts.Account;
import l2.authserver.accounts.SessionManager;
import l2.authserver.accounts.SessionManager.Session;
import l2.authserver.network.l2.L2LoginClient;
import l2.authserver.network.l2.L2LoginClient.LoginClientState;
import l2.authserver.network.l2.s2c.LoginFail.LoginFailReason;
import l2.authserver.network.l2.s2c.LoginOk;
import l2.authserver.utils.Log;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;

import static l2.authserver.crypt.PasswordHash.compare;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class RequestAuthLogin extends L2LoginClientPacket {
  private byte[] _raw = new byte[128];

  private static Cipher rsaCipher;

  static {
    try {
      rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      log.error("static init: eMessage={}", e.getMessage());
    }
  }

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
      rsaCipher.init(2, client.getRSAPrivateKey());
      decrypted = rsaCipher.doFinal(this._raw, 0, 128);
    } catch (Exception e) {
      log.error("Exception: eMessage={}, eClass={}, eCause={}", e.getMessage(), this.getClass().getSimpleName(), e.getCause());
      client.closeNow(true);
      return;
    }

    var requestLogin = (new String(decrypted, 94, 14)).trim();
    var requestPassword = (new String(decrypted, 108, 16)).trim();
    int currentTime = (int) (System.currentTimeMillis() / 1000L);

    Account account = new Account(requestLogin);
    account.restore();

    if (account.getPasswordHash() == null) {
      if (!Config.AUTO_CREATE_ACCOUNTS || !requestLogin.matches(Config.ANAME_TEMPLATE) || !requestPassword.matches(Config.APASSWD_TEMPLATE)) {
        client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
        return;
      }
      account.setPasswordHash(requestPassword);
      account.save();
    }

    boolean passwordCorrect = compare(requestPassword, account.getPasswordHash());

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
      client.setLogin(requestLogin);
      client.setAccount(account);
      client.setSessionKey(session.getSessionKey());
      client.setState(LoginClientState.AUTHED);
      client.sendPacket(new LoginOk(client.getSessionKey()));
    }
  }
}
