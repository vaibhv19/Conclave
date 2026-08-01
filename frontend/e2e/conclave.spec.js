import { test, expect } from '@playwright/test';

test.describe('Conclave E2E Flow', () => {
    test.beforeEach(async ({ page }) => {
        // Mock Auth Endpoints
        await page.route('**/api/auth/login', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    token: 'mock-jwt-token',
                    user: { id: 'user-1', name: 'Developer User', email: 'dev@conclave.ai' }
                })
            });
        });

        await page.route('**/api/auth/register', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    token: 'mock-jwt-token',
                    user: { id: 'user-1', name: 'Developer User', email: 'dev@conclave.ai' }
                })
            });
        });

        // Mock Room Endpoints
        await page.route('**/api/rooms', async (route) => {
            if (route.request().method() === 'POST') {
                await route.fulfill({
                    status: 201,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        roomId: '99999999-9999-9999-9999-999999999999',
                        name: 'Tactical Campaign Room',
                        objective: 'Draft and review a slogan for a new AI workspace',
                        status: 'INITIALIZED',
                        roleAssignments: [
                            { roleName: 'Lead-Writer', modelId: 'LLAMA3', uiColorHex: '#6366f1' },
                            { roleName: 'Code-Critic', modelId: 'MISTRAL', uiColorHex: '#f59e0b' }
                        ],
                        workflowState: {
                            currentDraft: '',
                            reviewComments: '',
                            lastUpdatedAt: null
                        }
                    })
                });
            }
        });

        await page.route('**/api/rooms/99999999-9999-9999-9999-999999999999', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    roomId: '99999999-9999-9999-9999-999999999999',
                    name: 'Tactical Campaign Room',
                    objective: 'Draft and review a slogan for a new AI workspace',
                    status: 'INITIALIZED',
                    roleAssignments: [
                        { roleName: 'Lead-Writer', modelId: 'LLAMA3', uiColorHex: '#6366f1' },
                        { roleName: 'Code-Critic', modelId: 'MISTRAL', uiColorHex: '#f59e0b' }
                    ],
                    workflowState: {
                        currentDraft: 'Slogan: Orchestrate target model consensus.',
                        reviewComments: 'Critic: Review details verified.',
                        lastUpdatedAt: new Date().toISOString()
                    }
                })
            });
        });
    });

    test('should register, login, configure room, and open chat view', async ({ page }) => {
        // Go to local Vite server
        await page.goto('http://localhost:5173/');

        // 1. Assert we are on Login screen
        await expect(page.locator('h2')).toContainText('Welcome Back');

        // Toggle to Register Screen
        await page.click('button:has-text("Sign Up")');
        await expect(page.locator('h2')).toContainText('Create Account');

        // Fill out registration
        await page.fill('input[placeholder="John Doe"]', 'Developer User');
        await page.fill('input[placeholder="you@example.com"]', 'dev@conclave.ai');
        await page.fill('input[placeholder="••••••••"]', 'password123');
        await page.click('button:has-text("Create Account")');

        // 2. Assert transition to Room Setup Wizard
        await expect(page.locator('h1')).toContainText('Create Workspace');

        // Fill out Room creation details
        await page.fill('input[placeholder="e.g. Slogan Draft Campaign"]', 'Tactical Campaign Room');
        await page.fill('textarea[placeholder^="State the objective"]', 'Draft and review a slogan for a new AI workspace');
        
        // Assert pre-populated roles
        await expect(page.locator('input[value="Lead-Writer"]')).toBeVisible();
        await expect(page.locator('input[value="Code-Critic"]')).toBeVisible();

        // Submit form
        await page.click('button:has-text("Initialize Consensus Workspace")');

        // 3. Assert transition to Room Chat panel
        await expect(page.locator('header h2')).toContainText('Tactical Campaign Room');
        
        // Assert objective rendering inside Sidebar
        await expect(page.locator('aside')).toContainText('Draft and review a slogan for a new AI workspace');

        // Assert input box renders successfully
        await expect(page.locator('textarea[placeholder^="Type a prompt"]')).toBeVisible();
    });
});
