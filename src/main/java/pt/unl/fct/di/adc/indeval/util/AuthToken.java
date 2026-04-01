package pt.unl.fct.di.adc.indeval.util;

import java.util.UUID;

public class AuthToken {

	public static final long ACTIVE_TIME = 1000*60*15; // 15 min
	
	public String tokenID;
	public String username;
	public Role role;
	public long issuedAt;
	public long expiresAt;
	
	public AuthToken() { }
	
	public AuthToken(String username, Role role) {

		this.username = username;
		this.role = role;

		this.tokenID = UUID.randomUUID().toString();
		this.issuedAt = System.currentTimeMillis();
		this.expiresAt = this.issuedAt + ACTIVE_TIME;
	}
	
}
