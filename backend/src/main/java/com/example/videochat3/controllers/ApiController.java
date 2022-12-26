package com.example.videochat3.controllers;

import com.example.videochat3.domain.Meeting;
import com.example.videochat3.recaptcha.RecaptchaManager;
import com.example.videochat3.DTO.MeetingDTO;
import com.example.videochat3.DTO.MeetingUpdateDTO;
import com.example.videochat3.DTO.PasswordResetDTO;
import com.example.videochat3.DTO.HostTokenDTO;
import com.example.videochat3.domain.AppUser;
import com.example.videochat3.service.AppUserService;
import com.example.videochat3.service.EmailService;
import com.example.videochat3.service.MeetingService;
import lombok.RequiredArgsConstructor;

import com.aventrix.jnanoid.jnanoid.*;

import org.apache.catalina.connector.Response;
import org.passay.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.videochat3.tokens.UserTokenManager;
import com.example.videochat3.DTO.EmailDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.Principal;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Controller
@RequiredArgsConstructor
public class ApiController {


    private final AppUserService appUserService;
    private final MeetingService meetingService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RecaptchaManager recaptchaManager;

    @GetMapping("/api/users/userinfo")
    public ResponseEntity userInfo(Principal principal) {
        String email = principal.getName();
        AppUser user = appUserService.findAppUserByEmail(email);
        Map<String, String> publicUserInfo = new HashMap<>();
        publicUserInfo.put("email", user.getEmail());
        publicUserInfo.put("name", user.getName());
        return ResponseEntity.ok().body(publicUserInfo);
    }

    @GetMapping("/api/token/refresh")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String refresh_token = authorizationHeader.substring("Bearer ".length());
                Map<String, String> tokens = UserTokenManager.refreshAccessToken(refresh_token, appUserService);
                response.setContentType(APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), tokens);
            } catch (Exception e) {
                response.setHeader("error", e.getMessage());
                response.setStatus(403);
                Map<String, String> error = new HashMap<>();
                error.put("error_message", e.getMessage());
                response.setContentType(APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), error);
            }
        } else {
            throw new RuntimeException("Refresh token is missing.");
        }
    }

    @PostMapping("/api/users/request_password_reset")
    public ResponseEntity requestPasswordReset(@RequestParam String email, @RequestParam String recaptchaToken) throws IOException {
        if(recaptchaManager.verifyRecaptchaToken(recaptchaToken)) {
            AppUser user = appUserService.findAppUserByEmail(email);
            if(user == null) return ResponseEntity.notFound().build();
            else {
                String passwordResetURI = NanoIdUtils.randomNanoId();

                CharacterRule digits = new CharacterRule(EnglishCharacterData.Digit);
                CharacterRule upperCase = new CharacterRule(EnglishCharacterData.UpperCase);
                CharacterRule lowerCase = new CharacterRule(EnglishCharacterData.LowerCase);

                //these should be hashed
                PasswordGenerator passwordGenerator = new PasswordGenerator();
                String passwordResetCode = passwordGenerator.generatePassword(8, digits, upperCase, lowerCase);

                appUserService.setUserPasswordResetCodes(user.getId(), passwordEncoder.encode(passwordResetURI), passwordEncoder.encode(passwordResetCode));

                String messageBody = "Dear Sia User,\n\nSomeone has requested a password reset link for your account. If this wasn't you, no action needs to be taken. " +
                "If this was you, please go to the following link:\n\n" +
                "http://localhost:4200/resetpassword/" + passwordResetURI + "\n\n" +
                "and enter the following password:\n\n" + passwordResetCode + "\n\n" + 
                "Thank you.\n\nThe Sia Team";

                EmailDetails appToUserEmailDetails = new EmailDetails(user.getEmail(), messageBody, "Password Reset Request", "");
                emailService.sendSimpleMail(appToUserEmailDetails);

                return ResponseEntity.ok().build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping(value ="/api/users/reset_password",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity resetPassword(@RequestBody PasswordResetDTO passwordResetDTO) {
        if(recaptchaManager.verifyRecaptchaToken(passwordResetDTO.getRecaptchaToken())) {
        AppUser user = appUserService.findAppUserByEmail(passwordResetDTO.getEmail());
            if(user == null) return ResponseEntity.notFound().build();
            else if(
                user.getPasswordResetCode().length() > 0 &&
                user.getPasswordResetURI().length() > 0 &&
                passwordEncoder.matches(passwordResetDTO.getPasswordResetURI(), user.getPasswordResetURI()) &&
                passwordEncoder.matches(passwordResetDTO.getPasswordResetCode(), user.getPasswordResetCode())
            ) {
                appUserService.resetUserPassword(user.getId(), passwordEncoder.encode(passwordResetDTO.getNewPassword()));
                return ResponseEntity.ok().build();
            } else {
                System.out.println(passwordEncoder.matches(passwordResetDTO.getPasswordResetURI(), user.getPasswordResetURI()));
                System.out.println(passwordEncoder.matches(passwordResetDTO.getPasswordResetCode(), user.getPasswordResetCode()));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping(
            value = "/api/users/new_meeting",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity createNewMeeting(@RequestBody MeetingDTO meetingDTO, Principal principal) {
        if(meetingDTO.getTitle() == null || meetingDTO.getPassword() == null || meetingDTO.getDuration() == null || meetingDTO.getStartDateTime() == null) {
            return new ResponseEntity("Missing fields.", HttpStatus.BAD_REQUEST);
        }
        String email = principal.getName();
        AppUser user = appUserService.findAppUserByEmail(email);

        Meeting meeting = 
            new Meeting(
                null, 
                meetingDTO.getTitle(), 
                meetingDTO.getPassword(), 
                meetingDTO.getDuration(), 
                new Date(meetingDTO.getStartDateTime()),
                new ArrayList<String>(meetingDTO.getGuests()),
                user.getId().toString());
        meeting = meetingService.saveMeeting(meeting);
        DateFormat DFormat = DateFormat.getDateTimeInstance(
            DateFormat.LONG, DateFormat.LONG,
            Locale.getDefault());
        //email guests that the meeting has been canceled, then
        for(String guest : meeting.getGuests()) {
            String messageBody = 
            "Dear " + guest + ",\n\n" +
            user.getName() + " has invited you to join a Sia video meeting:\n\n" +
            meeting.getTitle() + "\n" +
            "Scheduled for " + DFormat.format(meeting.getStartDateTime()) + "\n\n" +
            "To join this meeting, visit:\n\n" + 
            "http://localhost:4200/joinmeeting?id=" + meeting.getId() + "\n\n" +
            "And enter the password:\n\n" + 
            meetingDTO.getPassword() + "\n\n" +
            "Thank you,\n" +
            "The Sia Team";
            EmailDetails emailDetails = new EmailDetails(guest, messageBody, "New Sia Meeting Invitation", "");
            this.emailService.sendSimpleMail(emailDetails);
        }
        return ResponseEntity.ok().body(meeting);
    }

    //get all of a user's meetings, this will eventually change and use indexedDB in the frontend
    @GetMapping("/api/users/meetings")
    public ResponseEntity<List<Meeting>> getMeetings(Principal principal, @RequestParam Long startDate, @RequestParam Long endDate) {
        String email = principal.getName();
        AppUser user = appUserService.findAppUserByEmail(email);
        return ResponseEntity.ok().body(meetingService.getMeetings(user.getId().toString(), new Date(startDate), new Date(endDate)));
    }

    @PutMapping(
        value = "/api/users/update_meeting",
        consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
    )
    public ResponseEntity updateMeeting(@RequestBody MeetingUpdateDTO meetingUpdateDTO, Principal principal) {
        Meeting m = meetingService.getMeeting(meetingUpdateDTO.getId());
        AppUser user = appUserService.findAppUserByEmail(principal.getName());
        System.out.println("we are here");
        if(!m.getOwnerId().equals(user.getId().toString())) {
            System.out.println("forbidden");
            return new ResponseEntity("Forbidden.", HttpStatus.FORBIDDEN);
        } else {
            DateFormat DFormat = DateFormat.getDateTimeInstance(
                DateFormat.LONG, DateFormat.LONG,
                Locale.getDefault());
            //email guests that the meeting has been canceled, then
            for(String guest : m.getGuests()) {
            String messageBody = 
            "Dear Sia Guest,\n\n" +
            user.getName() + " has updated a meeting you were invited to.\n\nOld meeting details:\n\n" +
            m.getTitle() + "\n" +
            "Scheduled for: " + DFormat.format(m.getStartDateTime()) + "\n" +
            "Length: " + m.getDuration() + " minutes\n\n" +
            "New meeting details:\n\n" +
            meetingUpdateDTO.getTitle() + "\n" +
            "Scheduled for: " + DFormat.format(new Date(meetingUpdateDTO.getStartDateTime())) + "\n" +
            "Length: " + meetingUpdateDTO.getDuration() + " minutes\n" +
            "Password: " + meetingUpdateDTO.getPassword() + "\n\n" + 
            "To join this meeting, visit:\n\n" + 
            "http://localhost:4200/joinmeeting?id=" + m.getId() + "\n\n" +
            "And enter the password listed above.\n\n" +
            "Thank you,\n" +
            "The Sia Team";
            EmailDetails emailDetails = new EmailDetails(guest, messageBody, "Updated Sia Meeting Invitation", "");
            this.emailService.sendSimpleMail(emailDetails);
        }
            //email users that the meeting has been update
            meetingService.updateMeeting(
                meetingUpdateDTO.getTitle(),
                meetingUpdateDTO.getPassword(), 
                meetingUpdateDTO.getDuration(), 
                new Date(meetingUpdateDTO.getStartDateTime()), 
                meetingUpdateDTO.getId()
            );
            return ResponseEntity.ok().build();
        }
    }

    @DeleteMapping("/api/users/delete_meeting") 
    public ResponseEntity deleteMeeting(Principal principal, @RequestParam String id) {
        Meeting m = meetingService.getMeeting(id);
        AppUser user = appUserService.findAppUserByEmail(principal.getName());
        if(!m.getOwnerId().equals(user.getId().toString())) {
            return new ResponseEntity("Forbidden.", HttpStatus.FORBIDDEN);
        } else {
            DateFormat DFormat = DateFormat.getDateTimeInstance(
            DateFormat.LONG, DateFormat.LONG,
            Locale.getDefault());
            //email guests that the meeting has been canceled, then
            for(String guest : m.getGuests()) {
                String messageBody = 
                "Dear Sia Guest,\n\n" +
                user.getName() + " has canceled the following meeting:\n\n" +
                m.getTitle() + "\n" +
                "Scheduled for " + DFormat.format(m.getStartDateTime()) + "\n\n" +
                "Thank you,\n" +
                "The Sia Team";
                EmailDetails emailDetails = new EmailDetails(guest, messageBody, "Meeting Canceled", "");
                this.emailService.sendSimpleMail(emailDetails);
            }
            meetingService.deleteMeetingById(id);
            return ResponseEntity.ok().build();
        }
    }

    //create host token
    @PostMapping("/api/users/host_token")
    public ResponseEntity generateHostToken(Principal principal, @RequestBody HostTokenDTO hostTokenDTO) {
        String meetingId = hostTokenDTO.getMeetingId();
        String email = principal.getName();
        AppUser user = appUserService.findAppUserByEmail(email);
        Meeting meeting = this.meetingService.getMeeting(meetingId);
        if(!meeting.getOwnerId().equals(user.getId().toString())) {
            return new ResponseEntity("Forbidden.", HttpStatus.FORBIDDEN);
        } else {
            User host = meetingService.loadHostByMeetingId(meetingId);
            //tokenize the host and send
            Map<String,String> meetingToken = UserTokenManager.meetingUserToTokenMap(host);
            return ResponseEntity.ok().body(meetingToken);
        }
    }
}
