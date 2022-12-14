import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { faAngleLeft } from '@fortawesome/free-solid-svg-icons';
import { Meeting } from 'src/app/models/meeting.model';
import { DateTimeService } from 'src/app/services/date-time/date-time.service';
import { MeetingsService } from 'src/app/services/meetings/meetings.service';
import { LoadingService } from 'src/app/services/loading/loading.service';

@Component({
  selector: 'app-edit-meeting',
  templateUrl: './edit-meeting.component.html',
  styleUrls: ['./edit-meeting.component.scss']
})
export class EditMeetingComponent implements OnChanges {
  editSucceeded = false;
  editSuccessText = '';
  deleteSucceeded = false;
  deleteSuccessText = '';
  serverError = '';
  
  @Input() meeting?:Meeting;

  editMeetingForm = new FormGroup({
    'title': new FormControl(this.meeting ? this.meeting.title : null, [Validators.required]),
    'startDateTime': new FormControl(this.meeting ? this.dateTimeService.convertToFormInputValue(this.meeting.startDateTime) : null, [Validators.required]),
    'duration': new FormControl(this.meeting ? this.meeting.duration : null, [Validators.required, Validators.pattern(/\d+/), Validators.min(5), Validators.max(120)])
  });

  @Output() viewModeActivated = new EventEmitter<void>();
  @Output() goBack = new EventEmitter<void>();


  faAngleLeft = faAngleLeft;

  constructor(
    public dateTimeService:DateTimeService, 
    public meetingsService:MeetingsService,
    public loadingService:LoadingService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if(this.meeting) {
      this.editMeetingForm.setValue({
        title : this.meeting.title,
        startDateTime: this.dateTimeService.convertToFormInputValue(this.meeting.startDateTime),
        duration: this.meeting.duration
      });
    }
  }

  async onDelete() {
    if(this.meeting) {
      const confirmDelete = window.confirm("Are you sure you would like to permanently delete this meeting?");
      if(confirmDelete) {
        this.loadingService.isLoading = true;
          try {
            const successMessage = await this.meetingsService.deleteMeeting(this.meeting.id);
            this.deleteSuccessText = successMessage;
            this.deleteSucceeded = true;
            this.loadingService.isLoading = false;
          } catch(e) {
            this.serverError = 'There was a problem deleting the meeting.';
            this.loadingService.isLoading = false;
          }
      }
    }
  }

  async onSubmit() {
    if(
      this.editMeetingForm.valid &&
      this.editMeetingForm.value.title && 
      this.editMeetingForm.value.startDateTime &&
      this.editMeetingForm.value.duration &&
      this.meeting
    ) {
      this.loadingService.isLoading = true;
      try {
        const successMessage = await this.meetingsService.updateMeeting(
          this.meeting.id, 
          this.editMeetingForm.value.title,
          this.editMeetingForm.value.startDateTime,
          this.editMeetingForm.value.duration
        );
        this.editSuccessText = successMessage;
        this.editSucceeded = true;
        this.loadingService.isLoading = false;
      } catch(e) {
        this.serverError = 'There was a problem updating the meeting.';
        this.loadingService.isLoading = false;
      }
    }
  }

  onEnterViewMode(event:Event) {
    event.preventDefault();
    this.viewModeActivated.emit();
  }
  
}
