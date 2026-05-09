import { TestBed } from '@angular/core/testing';

import { PastesService } from './pastes.service';

describe('PastesService', () => {
  let service: PastesService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PastesService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
