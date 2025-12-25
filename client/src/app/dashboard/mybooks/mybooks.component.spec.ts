import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MybooksComponent } from './mybooks.component';

describe('MybooksComponent', () => {
  let component: MybooksComponent;
  let fixture: ComponentFixture<MybooksComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [MybooksComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MybooksComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
