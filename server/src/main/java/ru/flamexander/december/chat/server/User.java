package ru.flamexander.december.chat.server;

import org.sqlite.date.DateFormatUtils;

import java.util.Date;

/**
 * @author zhechtec
 */
public class User {
	private Long id;
	protected String login;
	private String password;
	protected String role;
	/** дата, до которой пользователь забанен YYYY-MM-DD HH:MM:SS */
	private Date unbanTime;

	public User(String login, String password, String role) {
		this.login = login;
		this.password = password;
		this.role = role;
	}

	public User(String login, String password, String role, Date unbanTime) {
		this.login = login;
		this.password = password;
		this.role = role;
		this.unbanTime = unbanTime == null ? new Date() : unbanTime;
	}

	public User(String login, String password) {
		this.login = login;
		this.password = password;
		this.role = Role.USER.getTitle();
	}

	public void setRole(String role) {
		this.role = role;

	}

	public Long getId() {
		return id;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public boolean isAdmin(){
		return role.equals(Role.ADMIN.title);
	}

	public boolean isModerator(){
		return role.equals(Role.MODERATOR.title);
	}

	public String getRole(){
		return role;
	}

	@Override
	public String toString() {
		return String.format("Пользователь [id: %d, логин: %s, роль: %s, %s]", id, login, role,
				isBanned() ? "забанен до: " + DateFormatUtils.format(unbanTime, "yyyy-MM-dd HH:mm:ss") : "активен");
	}

	/**
	 * Роли, доступные для пользователей.
	 */
	enum Role {
		ADMIN("admin"), USER("user"), MODERATOR("moderator");

		private String title;

		Role(String title) {
			this.title = title;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}


	public void setUnbanTime(Date unbanTime){
		this.unbanTime = unbanTime;
	}

	public Date getUnbanTime() {
		return unbanTime;
	}

	public boolean isBanned(){
		return unbanTime != null && System.currentTimeMillis() < unbanTime.getTime();
	}
}
