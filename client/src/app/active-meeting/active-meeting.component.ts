import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { SignalingService } from '../services/signaling/signaling.service';
import { Message } from '../models/message.model';

@Component({
  selector: 'app-active-meeting',
  templateUrl: './active-meeting.component.html',
  styleUrls: ['./active-meeting.component.scss']
})
export class ActiveMeetingComponent implements OnInit {
  messages:Message[] = [];
  newMessage = '';

  constructor(public signalingService:SignalingService, private changeDetection:ChangeDetectorRef) { }

  ngOnInit(): void {
    this.messages = this.signalingService.getMessages();
    this.signalingService.receivedNewMessage.subscribe(() => {
      console.log("event fired");
      this.messages = this.signalingService.getMessages();
      this.changeDetection.detectChanges();
    });
  }

  onSendMessage() {
    this.signalingService.broadCastMessage(this.newMessage);
    this.newMessage = '';
  }

}