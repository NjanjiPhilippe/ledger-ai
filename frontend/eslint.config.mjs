// @ts-check
import angular from 'angular-eslint';
import boundaries from 'eslint-plugin-boundaries';
import tseslint from 'typescript-eslint';

/**
 * Architecture rules of the frontend (see docs/adr/0003-frontend-architecture.md), checked like ArchUnit checks
 * the backend:
 *   - core and shared never depend on a feature, and a feature never depends on another feature;
 *   - inside a feature: ui is presentation only, data-access only talks HTTP, state sits between pages and data-access;
 *   - HttpClient is reachable only from data-access and core (no-restricted-imports below).
 */
export default tseslint.config(
  { ignores: ['dist/**', '.angular/**', 'coverage/**', 'src/app/core/api/schema.ts'] },
  {
    files: ['**/*.ts'],
    extends: [...tseslint.configs.recommended, ...angular.configs.tsRecommended],
    processor: angular.processInlineTemplates,
    plugins: { boundaries },
    settings: {
      // The plugin resolves imports itself; by default it only looks for .js files, and ours are TypeScript.
      'import/resolver': { node: { extensions: ['.ts', '.js'] } },
      'boundaries/elements': [
        // The most specific descriptors first: the first match decides the type of a file.
        { type: 'feature-pages', pattern: 'src/app/features/*/pages', capture: ['feature'] },
        { type: 'feature-ui', pattern: 'src/app/features/*/ui', capture: ['feature'] },
        { type: 'feature-state', pattern: 'src/app/features/*/state', capture: ['feature'] },
        {
          type: 'feature-data-access',
          pattern: 'src/app/features/*/data-access',
          capture: ['feature'],
        },
        { type: 'core', pattern: 'src/app/core' },
        { type: 'shared', pattern: 'src/app/shared' },
      ],
    },
    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        { type: 'attribute', prefix: 'app', style: 'camelCase' },
      ],
      '@angular-eslint/component-selector': [
        'error',
        { type: 'element', prefix: 'app', style: 'kebab-case' },
      ],
      'boundaries/dependencies': [
        2,
        {
          default: 'allow',
          policies: [
            {
              from: { element: { types: { anyOf: ['core', 'shared'] } } },
              disallow: {
                to: {
                  element: {
                    types: {
                      anyOf: [
                        'feature-pages',
                        'feature-ui',
                        'feature-state',
                        'feature-data-access',
                      ],
                    },
                  },
                },
              },
            },
            {
              from: {
                element: {
                  types: {
                    anyOf: ['feature-pages', 'feature-ui', 'feature-state', 'feature-data-access'],
                  },
                },
              },
              disallow: {
                to: {
                  element: {
                    types: {
                      anyOf: [
                        'feature-pages',
                        'feature-ui',
                        'feature-state',
                        'feature-data-access',
                      ],
                    },
                    captured: { feature: '!{{ from.captured.feature }}' },
                  },
                },
              },
            },
            {
              from: { element: { type: 'feature-ui' } },
              disallow: {
                to: {
                  element: {
                    types: { anyOf: ['feature-pages', 'feature-state', 'feature-data-access'] },
                  },
                },
              },
            },
            {
              from: { element: { type: 'feature-data-access' } },
              disallow: {
                to: {
                  element: { types: { anyOf: ['feature-pages', 'feature-ui', 'feature-state'] } },
                },
              },
            },
            {
              from: { element: { type: 'feature-state' } },
              disallow: { to: { element: { types: { anyOf: ['feature-pages', 'feature-ui'] } } } },
            },
          ],
        },
      ],
    },
  },
  {
    // HttpClient is reachable only from data-access and core (native rule: it needs no import resolution).
    files: ['src/app/features/*/{pages,ui,state}/**/*.ts', 'src/app/shared/**/*.ts'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          paths: [
            {
              name: '@angular/common/http',
              message:
                'Only data-access (and core) talk HTTP: go through the feature data-access service.',
            },
          ],
        },
      ],
    },
  },
  {
    files: ['**/*.html'],
    extends: [...angular.configs.templateRecommended, ...angular.configs.templateAccessibility],
    rules: {},
  },
);
