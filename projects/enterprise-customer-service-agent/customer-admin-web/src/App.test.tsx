import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import App from './App';

const chatResponse = {
  traceId: 'trace-live',
  route: 'ORDER_LOOKUP',
  riskLevel: 'READ_ONLY',
  answer: '模型回复：订单已支付，下周一开课。',
  sources: ['order:order-1001'],
  nextActions: ['展示订单状态', '等待用户继续追问']
};

const orderResponse = {
  id: 'order-1001',
  tenantId: 'tenant-demo',
  customerId: 'customer-1001',
  productName: '企业级 AI Agent 实战营',
  status: 'PAID',
  paidAt: '2026-06-01T10:00:00Z'
};

beforeAll(() => {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
      addEventListener: vi.fn(),
      addListener: vi.fn(),
      dispatchEvent: vi.fn(),
      matches: false,
      media: query,
      onchange: null,
      removeEventListener: vi.fn(),
      removeListener: vi.fn()
    }))
  });
  globalThis.ResizeObserver = class {
    disconnect() {}

    observe() {}

    unobserve() {}
  };
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('App', () => {
  it('renders the interactive debug console shell', () => {
    const html = renderToString(<App />);

    expect(html).toContain('Customer Agent Debug Console');
    expect(html).toContain('Service UP');
    expect(html).toContain('order-1001');
    expect(html).toContain('企业级 AI Agent 实战营');
    expect(html).toContain('ORDER_LOOKUP');
    expect(html).toContain('READ_ONLY');
  });

  it('sends custom chat messages from the debug console', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === '/health') {
        return jsonResponse({ status: 'UP', service: 'customer-agent-app' });
      }
      if (path === '/api/orders/order-1001') {
        return jsonResponse(orderResponse);
      }
      if (path === '/chat') {
        expect(init?.method).toBe('POST');
        expect(JSON.parse(String(init?.body))).toEqual({
          tenantId: 'tenant-demo',
          message: '帮我查订单 order-1001'
        });
        return jsonResponse(chatResponse);
      }
      throw new Error(`unexpected request: ${path}`);
    });

    render(<App />);

    const user = userEvent.setup();
    await user.clear(screen.getByLabelText('用户消息'));
    await user.type(screen.getByLabelText('用户消息'), '帮我查订单 order-1001');
    await user.click(screen.getByRole('button', { name: '发送' }));

    await waitFor(() => expect(screen.getByText('模型回复：订单已支付，下周一开课。')).toBeTruthy());
    expect(screen.getByText('trace-live')).toBeTruthy();
    expect(fetchMock).toHaveBeenCalledWith(
      '/chat',
      expect.objectContaining({
        method: 'POST'
      })
    );
  });
});

function jsonResponse(body: unknown) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      headers: {
        'Content-Type': 'application/json'
      },
      status: 200
    })
  );
}
