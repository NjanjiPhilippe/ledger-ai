import { ToastService } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    vi.useFakeTimers();
    service = new ToastService();
  });

  afterEach(() => vi.useRealTimers());

  it('shows a toast with its level, title and detail', () => {
    service.error('Title', 'Detail');

    expect(service.toasts()).toEqual([
      expect.objectContaining({ level: 'error', title: 'Title', detail: 'Detail' }),
    ]);
  });

  it('dismisses each level after its own duration, errors lasting the longest', () => {
    service.success('ok');
    service.error('ko');

    vi.advanceTimersByTime(4000);
    expect(service.toasts().map((t) => t.title)).toEqual(['ko']);

    vi.advanceTimersByTime(4000);
    expect(service.toasts()).toEqual([]);
  });

  it('does not stack a message identical to one on screen: it restarts its timer instead', () => {
    service.error('Same', 'detail');
    vi.advanceTimersByTime(6000);
    service.error('Same', 'detail');

    expect(service.toasts()).toHaveLength(1);
    vi.advanceTimersByTime(6000); // would have expired 4000 ms ago without the restart
    expect(service.toasts()).toHaveLength(1);
    vi.advanceTimersByTime(2000);
    expect(service.toasts()).toEqual([]);
  });

  it('keeps toasts that differ only by detail', () => {
    service.error('Same', 'one');
    service.error('Same', 'two');

    expect(service.toasts()).toHaveLength(2);
  });

  it('shows at most five toasts, dropping the oldest', () => {
    for (let i = 1; i <= 7; i++) {
      service.info(`toast ${i}`);
    }

    expect(service.toasts().map((t) => t.title)).toEqual([
      'toast 3',
      'toast 4',
      'toast 5',
      'toast 6',
      'toast 7',
    ]);
  });

  it('dismisses on request and cancels the timer', () => {
    service.warning('careful');
    const [toast] = service.toasts();

    service.dismiss(toast.id);

    expect(service.toasts()).toEqual([]);
    expect(vi.getTimerCount()).toBe(0);
  });
});
