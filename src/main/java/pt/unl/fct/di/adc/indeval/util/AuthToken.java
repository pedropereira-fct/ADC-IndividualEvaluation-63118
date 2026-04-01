package pt.unl.fct.di.adc.indeval.util;

import java.util.UUID;

public class AuthToken {

	public static final long ACTIVE_TIME = 1000*60*15; // 15 min
	
	public String tokenId;
	public String username;
	public Role role;
	public Long issuedAt;
	public Long expiresAt;
	
	public AuthToken() { }
	
	public AuthToken(String username, Role role) {

		this.username = username;
		this.role = role;

		this.tokenId = UUID.randomUUID().toString();
		this.issuedAt = System.currentTimeMillis();
		this.expiresAt = this.issuedAt + ACTIVE_TIME;
	}
	
}
