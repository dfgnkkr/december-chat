package ru.flamexander.december.chat.server;

/**
 * @author zhechtec
 */
public class User {
	private Long id;
	protected String login;
	private String password;
	private String username;
	protected String role;

	public User(String login, String password, String username, String role) {
		this.login = login;
		this.password = password;
		this.username = username;
		this.role = role;
	}

	public User(String login, String password, String username) {
		this.login = login;
		this.password = password;
		this.username = username;
		this.role = Role.USER.getTitle();
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

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public boolean isAdmin(){
		return role.equals(Role.ADMIN.title);
	}

	@Override
	public String toString() {
		return String.format("Студент [id: %d, логин: %s, имя: %s, роль: %s]", id, login, username, role);
	}

	/**
	 * Роли, доступные для пользователей.
	 */
	enum Role {
		ADMIN("admin"), USER("user");

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
}
