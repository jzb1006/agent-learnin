import { describe, expect, it } from 'vitest';
import config from './vite.config';

describe('vite dev server proxy', () => {
  it('forwards admin knowledge APIs to the agent backend', () => {
    expect(config.server?.proxy).toEqual(
      expect.objectContaining({
        '/admin': 'http://127.0.0.1:8080'
      })
    );
  });
});
