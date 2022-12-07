import { Component, OnInit } from '@angular/core';
import { MeetingsService } from 'src/app/services/meetings/meetings.service';
import { Meeting } from 'src/app/models/meeting.model';
import { SignalingService } from 'src/app/services/signaling/signaling.service';

@Component({
  selector: 'app-meetings-list',
  templateUrl: './meetings-list.component.html',
  styleUrls: ['./meetings-list.component.scss']
})
export class MeetingsListComponent implements OnInit {
  meetings:Meeting[] = [];
  showModal = false;

  constructor(private meetingsService:MeetingsService, private signalingService:SignalingService) { }

  ngOnInit(): void {
    this.meetings = this.meetingsService.getMeetings();
    this.meetingsService.meetingsModified.subscribe((modifiedMeetings:Meeting[]) => {
      this.meetings = modifiedMeetings;
    });
  }

  onOpen(meeting:Meeting) {
    this.signalingService.authenticateAsHost(meeting.id);
    this.showModal = true;
  }

}
