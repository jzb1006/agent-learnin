import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import App from './App';

const chatResponse = {
  traceId: 'trace-live',
  conversationId: 'debug-session',
  memorySummary: '最近订单 order-1001；route=ORDER_LOOKUP；用户=帮我查订单 order-1001',
  route: 'ORDER_LOOKUP',
  riskLevel: 'READ_ONLY',
  answer: '模型回复：订单已支付，下周一开课。',
  sources: ['order:order-1001'],
  nextActions: ['展示订单状态', '等待用户继续追问'],
  toolCalls: [
    {
      name: 'order_lookup',
      arguments: {
        orderId: 'order-1001',
        tenantId: 'tenant-demo'
      },
      status: 'SUCCEEDED',
      riskLevel: 'READ_ONLY',
      durationMs: 12,
      resultSummary: 'order-1001 企业级 AI Agent 实战营 PAID'
    }
  ]
};

const orderResponse = {
  id: 'order-1001',
  tenantId: 'tenant-demo',
  customerId: 'customer-1001',
  productName: '企业级 AI Agent 实战营',
  status: 'PAID',
  paidAt: '2026-06-01T10:00:00Z'
};

const legacyOrderResponse = {
  id: 'order-legacy-paid',
  tenantId: 'tenant-demo',
  customerId: 'customer-1002',
  productName: '企业级 AI Agent 架构班',
  status: 'PAID',
  paidAt: '2026-02-01T10:00:00Z'
};

const knowledgeItemResponse = {
  itemId: 'faq-day20-api',
  tenantId: 'tenant-demo',
  indexedChunks: 1,
  skipped: false
};

const knowledgeReindexResponse = {
  documents: 6,
  indexedChunks: 8,
  skippedItems: 0
};

const knowledgeDeleteResponse = {
  itemId: 'faq-day20-api',
  tenantId: 'tenant-demo',
  deleted: true
};

const knowledgeItemsResponse = {
  items: [
    {
      itemId: 'faq-day20-api',
      tenantId: 'tenant-demo',
      category: 'FAQ',
      title: 'Day20 知识管理 API',
      source: 'day20#api',
      version: '2026-06-30',
      indexedChunks: 1,
      contentPreview: '知识库管理 API 新增知识后，无需重启服务即可被 RAG 检索命中。'
    }
  ]
};

const knowledgeSearchResponse = {
  query: '知识库管理',
  tenantId: 'tenant-demo',
  topK: 3,
  matches: [
    {
      itemId: 'faq-day20-api',
      title: 'Day20 知识管理 API',
      source: 'day20#api',
      tenant: 'tenant-demo',
      category: 'FAQ',
      content: '知识库管理 API 新增知识后，无需重启服务即可被 RAG 检索命中。',
      score: 0.91
    }
  ]
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
    expect(html).toContain('订单号');
    expect(html).toContain('查询订单');
    expect(html).toContain('order-1001');
    expect(html).toContain('企业级 AI Agent 实战营');
    expect(html).toContain('Request Inspector');
    expect(html).toContain('Route');
    expect(html).toContain('Risk Level');
    expect(html).toContain('Trace ID');
    expect(html).toContain('Tool Calls');
    expect(html).toContain('order_lookup');
    expect(html).toContain('ORDER_LOOKUP');
    expect(html).toContain('READ_ONLY');
    expect(html).toContain('Knowledge Debug');
    expect(html).toContain('重建索引');
  });

  it('queries orders from the visible order debug form', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === '/health') {
        return jsonResponse({ status: 'UP', service: 'customer-agent-app' });
      }
      if (path === '/api/orders/order-1001') {
        expect(init?.headers).toEqual(expect.objectContaining({ 'X-Tenant-ID': 'tenant-demo' }));
        return jsonResponse(orderResponse);
      }
      if (path === '/api/orders/order-legacy-paid') {
        expect(init?.headers).toEqual(expect.objectContaining({ 'X-Tenant-ID': 'tenant-demo' }));
        return jsonResponse(legacyOrderResponse);
      }
      throw new Error(`unexpected request: ${path}`);
    });

    render(<App />);

    const user = userEvent.setup();
    await user.clear(screen.getByLabelText('订单号'));
    await user.type(screen.getByLabelText('订单号'), 'order-legacy-paid');
    await user.click(screen.getByRole('button', { name: '查询订单' }));

    await waitFor(() => expect(screen.getByText('企业级 AI Agent 架构班')).toBeTruthy());
    expect(screen.getByText('order-legacy-paid')).toBeTruthy();
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/orders/order-legacy-paid',
      expect.objectContaining({
        headers: expect.objectContaining({ 'X-Tenant-ID': 'tenant-demo' })
      })
    );
  });

  it('does not retry missing order lookups', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === '/health') {
        return jsonResponse({ status: 'UP', service: 'customer-agent-app' });
      }
      if (path === '/api/orders/order-1001') {
        expect(init?.headers).toEqual(expect.objectContaining({ 'X-Tenant-ID': 'tenant-demo' }));
        return jsonResponse(orderResponse);
      }
      if (path === '/api/orders/missing-order') {
        expect(init?.headers).toEqual(expect.objectContaining({ 'X-Tenant-ID': 'tenant-demo' }));
        return errorResponse({
          errorCode: 'ORDER_NOT_FOUND',
          message: '订单不存在：missing-order',
          status: 404
        });
      }
      throw new Error(`unexpected request: ${path}`);
    });

    render(<App />);

    const user = userEvent.setup();
    await user.clear(screen.getByLabelText('订单号'));
    await user.type(screen.getByLabelText('订单号'), 'missing-order');
    await user.click(screen.getByRole('button', { name: '查询订单' }));

    await waitFor(() => expect(screen.getByText('订单不存在：missing-order')).toBeTruthy());
    expect(
      fetchMock.mock.calls.filter(([input]) => String(input) === '/api/orders/missing-order')
    ).toHaveLength(1);
  });

  it('sends custom chat messages from the debug console', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === '/health') {
        return jsonResponse({ status: 'UP', service: 'customer-agent-app' });
      }
      if (path === '/api/orders/order-1001') {
        expect(init?.headers).toEqual(expect.objectContaining({ 'X-Tenant-ID': 'tenant-demo' }));
        return jsonResponse(orderResponse);
      }
      if (path === '/chat') {
        expect(init?.method).toBe('POST');
        expect(init?.headers).toEqual(expect.objectContaining({ 'X-Tenant-ID': 'tenant-demo' }));
        expect(JSON.parse(String(init?.body))).toEqual({
          tenantId: 'tenant-demo',
          message: '帮我查订单 order-1001',
          conversationId: 'debug-session'
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
    expect(screen.getByText('Request Inspector')).toBeTruthy();
    expect(screen.getByText('Route')).toBeTruthy();
    expect(screen.getByText('ORDER_LOOKUP')).toBeTruthy();
    expect(screen.getByText('Risk Level')).toBeTruthy();
    expect(screen.getAllByText('READ_ONLY')).toHaveLength(2);
    expect(screen.getByText('Trace ID')).toBeTruthy();
    expect(screen.getByText('Conversation ID')).toBeTruthy();
    expect(screen.getByText('debug-session')).toBeTruthy();
    expect(screen.getByText('Memory')).toBeTruthy();
    expect(screen.getByText(/最近订单 order-1001/)).toBeTruthy();
    expect(screen.getByText('Tool Calls')).toBeTruthy();
    expect(screen.getByText('order_lookup')).toBeTruthy();
    expect(screen.getByText('orderId=order-1001')).toBeTruthy();
    expect(screen.getByText('12ms')).toBeTruthy();
    expect(screen.getByText('order-1001 企业级 AI Agent 实战营 PAID')).toBeTruthy();
    expect(screen.getByText('Next Actions')).toBeTruthy();
    expect(screen.getByText('展示订单状态')).toBeTruthy();
    expect(fetchMock).toHaveBeenCalledWith(
      '/chat',
      expect.objectContaining({
        method: 'POST'
      })
    );
  });

  it('upserts and reindexes knowledge from the debug panel', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const path = String(input);
      if (path === '/health') {
        return jsonResponse({ status: 'UP', service: 'customer-agent-app' });
      }
      if (path === '/api/orders/order-1001') {
        return jsonResponse(orderResponse);
      }
      if (path === '/admin/api/v1/knowledge/items') {
        if (init?.method === 'POST') {
          expect(init.headers).toEqual(expect.objectContaining({ 'X-Tenant-ID': 'tenant-demo' }));
          expect(JSON.parse(String(init.body))).toEqual(
            expect.objectContaining({
              itemId: 'faq-day20-api',
              category: 'FAQ',
              title: 'Day20 知识管理 API'
            })
          );
          return jsonResponse(knowledgeItemResponse);
        }
        expect(init?.headers).toEqual(expect.objectContaining({ 'X-Tenant-ID': 'tenant-demo' }));
        return jsonResponse(knowledgeItemsResponse);
      }
      if (path === '/admin/api/v1/knowledge/items?itemId=faq-day20-api') {
        expect(init?.method).toBe('DELETE');
        expect(init?.headers).toEqual(expect.objectContaining({ 'X-Tenant-ID': 'tenant-demo' }));
        return jsonResponse(knowledgeDeleteResponse);
      }
      if (path === '/admin/api/v1/knowledge/search?query=%E7%9F%A5%E8%AF%86%E5%BA%93%E7%AE%A1%E7%90%86&topK=3') {
        expect(init?.headers).toEqual(expect.objectContaining({ 'X-Tenant-ID': 'tenant-demo' }));
        return jsonResponse(knowledgeSearchResponse);
      }
      if (path === '/admin/api/v1/knowledge/reindex') {
        expect(init?.method).toBe('POST');
        expect(init?.headers).toEqual(expect.objectContaining({ 'X-Tenant-ID': 'tenant-demo' }));
        return jsonResponse(knowledgeReindexResponse);
      }
      throw new Error(`unexpected request: ${path}`);
    });

    render(<App />);

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '保存知识' }));
    await waitFor(() => expect(screen.getByText(/indexedChunks=1/)).toBeTruthy());
    expect(screen.getByText(/indexedChunks=1/)).toBeTruthy();
    const knowledgeItems = screen.getByLabelText('Knowledge Items');
    await waitFor(() => expect(within(knowledgeItems).getByText('Day20 知识管理 API')).toBeTruthy());
    expect(within(knowledgeItems).getByText('faq-day20-api')).toBeTruthy();

    await user.clear(screen.getByLabelText('知识搜索'));
    await user.type(screen.getByLabelText('知识搜索'), '知识库管理');
    await user.click(screen.getByRole('button', { name: '搜索知识' }));
    const searchResults = screen.getByLabelText('Knowledge Search Results');
    await waitFor(() => expect(within(searchResults).getByText(/score=0.91/)).toBeTruthy());
    expect(within(searchResults).getByText(/RAG 检索命中/)).toBeTruthy();

    await user.click(screen.getByRole('button', { name: '删除知识' }));
    await waitFor(() => expect(screen.getByText(/deleted=true/)).toBeTruthy());

    await user.click(screen.getByRole('button', { name: '重建索引' }));
    await waitFor(() => expect(screen.getByText(/documents=6/)).toBeTruthy());
    expect(screen.getByText(/indexedChunks=8/)).toBeTruthy();
    expect(fetchMock).toHaveBeenCalledWith(
      '/admin/api/v1/knowledge/reindex',
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

function errorResponse(body: unknown) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      headers: {
        'Content-Type': 'application/json'
      },
      status: 404
    })
  );
}
