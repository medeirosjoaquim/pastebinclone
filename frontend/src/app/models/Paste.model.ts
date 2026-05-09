import { Exposure } from './Exposure.enum';

export interface Paste {
  id: number;
  title: string;
  content: string;
  exposure: string;
  createdAt: string;
  updatedAt: string;
  expirationDate: string | null;
  url: string;
}

export interface CreatePaste {
  title: string;
  content: string;
  exposure: Exposure;
  expirationDate: string | null;
  password: string;
}
