package com.example.videochat3;

import com.example.videochat3.domain.AppUser;
import com.example.videochat3.domain.Meeting;
import com.example.videochat3.service.AppUserService;
import com.example.videochat3.service.MeetingService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.videochat3.controllers.LiveMeeting;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class Videochat3Application {

	public static void main(String[] args) {
		SpringApplication.run(Videochat3Application.class, args);
	}

	@Bean
	@Qualifier("UserPasswordEncoder")
	PasswordEncoder passwordEncoder() {
		Map<String, PasswordEncoder> encoders = new HashMap<String, PasswordEncoder>();
		encoders.put("bcrypt", new BCryptPasswordEncoder());
		DelegatingPasswordEncoder delegatingPasswordEncoder = new DelegatingPasswordEncoder("bcrypt", encoders);
		return delegatingPasswordEncoder;
	}

	@Bean
	ConcurrentHashMap<String, LiveMeeting> liveMeetings() {
		return new ConcurrentHashMap<String, LiveMeeting>();
	}

	@Bean
	CommandLineRunner run(AppUserService appUserService, MeetingService meetingService) {
		return args -> {
			appUserService.saveUser(new AppUser(null, "Joe", "jdvorakdevelops@gmail.com", "1234", "", ""));
		};
	}
}
