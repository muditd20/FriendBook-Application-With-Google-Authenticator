package com.friendbook.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String password;

	private String name;

	private String profilePhoto;

	private String totpSecret;

	@Column(name = "using_2fa", nullable = false, columnDefinition = "boolean default false")
	private boolean using2FA;

	@Transient
	private boolean alreadyFollowing;

	@Column(length = 500)
	private String favSong;

	@Column(length = 500)
	private String favBooks;

	@Column(length = 500)
	private String favPlaces;

	public String getFavSong() {
		return favSong;
	}

	public void setFavSong(String favSong) {
		this.favSong = favSong;
	}

	public String getFavBooks() {
		return favBooks;
	}

	public void setFavBooks(String favBooks) {
		this.favBooks = favBooks;
	}

	public String getFavPlaces() {
		return favPlaces;
	}

	public void setFavPlaces(String favPlaces) {
		this.favPlaces = favPlaces;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProfilePhoto() {
		return profilePhoto;
	}

	public void setProfilePhoto(String profilePhoto) {
		this.profilePhoto = profilePhoto;
	}

	public boolean isAlreadyFollowing() {
		return alreadyFollowing;
	}

	public void setAlreadyFollowing(boolean alreadyFollowing) {
		this.alreadyFollowing = alreadyFollowing;
	}

	@Transient
	private boolean requested = false;

	public boolean isRequested() {
		return requested;
	}

	public void setRequested(boolean requested) {
		this.requested = requested;
	}

	public String getTotpSecret() {
		return totpSecret;
	}

	public void setTotpSecret(String totpSecret) {
		this.totpSecret = totpSecret;
	}

	public boolean isUsing2FA() {
		return using2FA;
	}

	public void setUsing2FA(boolean using2FA) {
		this.using2FA = using2FA;
	}

}
