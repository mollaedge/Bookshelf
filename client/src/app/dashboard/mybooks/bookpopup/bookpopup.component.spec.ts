import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BookpopupComponent } from './bookpopup.component';

describe('BookpopupComponent', () => {
  let component: BookpopupComponent;
  let fixture: ComponentFixture<BookpopupComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [BookpopupComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BookpopupComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
