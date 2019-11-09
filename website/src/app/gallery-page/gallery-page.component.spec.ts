import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { GalleryPageComponent } from './gallery-page.component';

describe('GalleryPageComponent', () => {
  let component: GalleryPageComponent;
  let fixture: ComponentFixture<GalleryPageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ GalleryPageComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(GalleryPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
