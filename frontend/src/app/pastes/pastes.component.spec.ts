import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PastesComponent } from './pastes.component';

describe('PastesComponent', () => {
  let component: PastesComponent;
  let fixture: ComponentFixture<PastesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PastesComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(PastesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
