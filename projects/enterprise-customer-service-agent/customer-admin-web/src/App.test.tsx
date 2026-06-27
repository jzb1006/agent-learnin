import { renderToString } from 'react-dom/server';
import { describe, expect, it } from 'vitest';
import App from './App';

describe('App', () => {
  it('renders the Day 04 debug console with API snapshots', () => {
    const html = renderToString(<App />);

    expect(html).toContain('Customer Agent Debug Console');
    expect(html).toContain('Service UP');
    expect(html).toContain('order-1001');
    expect(html).toContain('企业级 AI Agent 实战营');
    expect(html).toContain('ORDER_LOOKUP');
    expect(html).toContain('READ_ONLY');
  });
});
