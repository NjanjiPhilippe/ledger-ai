import { LoadingService } from './loading.service';

describe('LoadingService', () => {
  it('is busy while at least one call is in flight', () => {
    const loading = new LoadingService();
    expect(loading.busy()).toBe(false);

    loading.start();
    loading.start();
    loading.stop();
    expect(loading.busy()).toBe(true);

    loading.stop();
    expect(loading.busy()).toBe(false);
  });

  it('never goes negative', () => {
    const loading = new LoadingService();
    loading.stop();
    loading.start();

    expect(loading.busy()).toBe(true);
  });
});
