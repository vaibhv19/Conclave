import React from 'react';
import { describe, test, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import MessageBubble from '../components/MessageBubble';
import TurnIndicator from '../components/TurnIndicator';
import ChatBar from '../components/ChatBar';
import Sidebar from '../components/Sidebar';
import AlertBanner from '../components/AlertBanner';

describe('MessageBubble Component', () => {
    test('renders user message correctly with right alignment', () => {
        const message = {
            id: '1',
            senderType: 'USER',
            content: 'Hello world',
            roleName: null,
            modelId: null
        };
        const { container } = render(<MessageBubble message={message} />);
        
        // Assert alignments
        expect(container.firstChild).toHaveClass('items-end');
        expect(screen.getByText('USER')).toBeInTheDocument();
        expect(screen.getByText('Hello world')).toBeInTheDocument();
    });

    test('renders AI model responses correctly with left alignment and color styling', () => {
        const message = {
            id: '2',
            senderType: 'AI',
            content: 'Consensus draft **v1**',
            roleName: 'Lead-Writer',
            modelId: 'LLAMA3',
            isMocked: false
        };
        const { container } = render(<MessageBubble message={message} roleColor="#ef4444" />);

        expect(container.firstChild).toHaveClass('items-start');
        expect(screen.getByText('Lead-Writer')).toBeInTheDocument();
        expect(screen.getByText('LLAMA3')).toBeInTheDocument();
        
        // Assert markdown bold rendering
        const boldElement = screen.getByText('v1');
        expect(boldElement.tagName).toBe('STRONG');
    });

    test('shows telemetry stats card on hover', async () => {
        const message = {
            id: '3',
            senderType: 'AI',
            content: 'Telemetry verification',
            roleName: 'Code-Critic',
            modelId: 'LLAMA3',
            isMocked: false
        };
        render(<MessageBubble message={message} />);
        
        const infoButton = screen.getByRole('button', { name: 'Audit metadata info' });
        expect(infoButton).toBeInTheDocument();

        // Initially hover card is not visible
        expect(screen.queryByText('Agent Telemetry')).not.toBeInTheDocument();

        // Simulate hover
        fireEvent.mouseEnter(infoButton);
        expect(screen.getByText('Agent Telemetry')).toBeInTheDocument();
        expect(screen.getByText('Ollama')).toBeInTheDocument();
        expect(screen.getAllByText('LLAMA3').length).toBe(2);

        // Mouse leave hides the popup
        fireEvent.mouseLeave(infoButton);
        expect(screen.queryByText('Agent Telemetry')).not.toBeInTheDocument();
    });
});

describe('TurnIndicator Component', () => {
    test('renders the pulsing streaming indicator correctly', () => {
        render(<TurnIndicator roleName="Lead-Writer" roleColor="#8b5cf6" />);
        
        expect(screen.getByText('@Lead-Writer is streaming')).toBeInTheDocument();
    });
});

describe('ChatBar Component', () => {
    const mockRoleAssignments = [
        { roleName: 'Lead-Writer', modelId: 'LLAMA3', uiColorHex: '#ef4444' },
        { roleName: 'Code-Critic', modelId: 'MISTRAL', uiColorHex: '#f59e0b' }
    ];

    test('allows entering message and triggers submit callback', () => {
        const handleSubmit = vi.fn();
        render(<ChatBar onSubmit={handleSubmit} roleAssignments={mockRoleAssignments} isPaused={false} />);
        
        const textarea = screen.getByPlaceholderText("Type a prompt (type '@' to select role)...");
        fireEvent.change(textarea, { target: { value: 'Draft a slogan' } });
        
        const sendButton = screen.getByRole('button', { name: 'Send Command' });
        fireEvent.click(sendButton);
        
        expect(handleSubmit).toHaveBeenCalledWith('Draft a slogan', false);
    });

    test('typing "@" character triggers mentions popover list', () => {
        render(<ChatBar onSubmit={vi.fn()} roleAssignments={mockRoleAssignments} isPaused={false} />);
        
        const textarea = screen.getByPlaceholderText("Type a prompt (type '@' to select role)...");
        
        // Typing @ triggers popover
        fireEvent.change(textarea, { target: { value: '@' } });
        
        expect(screen.getByText('Mention AI Role')).toBeInTheDocument();
        expect(screen.getByText('LLAMA3')).toBeInTheDocument();
        expect(screen.getByText('MISTRAL')).toBeInTheDocument();
    });

    test('selecting role from popover inserts it into input text', () => {
        render(<ChatBar onSubmit={vi.fn()} roleAssignments={mockRoleAssignments} isPaused={false} />);
        
        const textarea = screen.getByPlaceholderText("Type a prompt (type '@' to select role)...");
        fireEvent.change(textarea, { target: { value: 'Write to @' } });
        
        // Find the button inside popover by looking for modelId text
        const roleButtons = screen.getAllByRole('button');
        const popoverButton = roleButtons.find(btn => btn.textContent.includes('LLAMA3'));
        fireEvent.click(popoverButton);
        
        expect(textarea.value).toBe('Write to @Lead-Writer ');
    });

    test('displays Inject & Resume button styles when in PAUSED state', () => {
        const handleSubmit = vi.fn();
        render(<ChatBar onSubmit={handleSubmit} roleAssignments={mockRoleAssignments} isPaused={true} />);
        
        const textarea = screen.getByPlaceholderText("Pipeline HALTED. Provide feedback here to Inject & Resume...");
        fireEvent.change(textarea, { target: { value: 'Fix typos' } });
        
        const injectButton = screen.getByRole('button', { name: 'Inject & Resume' });
        expect(injectButton).toHaveClass('bg-amber-500');
        
        fireEvent.click(injectButton);
        expect(handleSubmit).toHaveBeenCalledWith('Fix typos', true);
    });
});

describe('Sidebar Component', () => {
    test('renders static objective, draft updates, and metrics correctly', () => {
        const workflowState = {
            currentDraft: 'Original draft context',
            reviewComments: 'No comments yet'
        };
        const tokenUsage = { promptTokens: 120, completionTokens: 85 };
        
        render(
            <Sidebar 
                objective="Deliver premium code" 
                workflowState={workflowState} 
                tokenUsage={tokenUsage} 
            />
        );

        expect(screen.getByText('Deliver premium code')).toBeInTheDocument();
        expect(screen.getByText('Original draft context')).toBeInTheDocument();
        expect(screen.getByText('No comments yet')).toBeInTheDocument();
        expect(screen.getByText('120')).toBeInTheDocument();
        expect(screen.getByText('85')).toBeInTheDocument();
    });

    test('supports collapsing and expanding via click toggle', () => {
        const { container } = render(
            <Sidebar objective="Objective test" />
        );
        
        const collapseButton = screen.getByRole('button', { name: 'Collapse sidebar' });
        expect(collapseButton).toBeInTheDocument();

        // Click to collapse
        fireEvent.click(collapseButton);
        expect(screen.getByRole('button', { name: 'Expand sidebar' })).toBeInTheDocument();
        expect(screen.queryByText('Room Objective')).not.toBeInTheDocument();
    });
});

describe('AlertBanner Component', () => {
    test('renders PAUSED status alerts and warning stripes when pipeline is halted', () => {
        const handleResume = vi.fn();
        render(
            <AlertBanner 
                isPaused={true} 
                currentRole="Code-Critic" 
                onResume={handleResume} 
                actionLoading={false} 
            />
        );
        
        expect(screen.getByText(/Pipeline execution halted/)).toBeInTheDocument();
        expect(screen.getByText(/at @Code-Critic/)).toBeInTheDocument();
        
        const resumeButton = screen.getByRole('button', { name: 'Force Resume' });
        fireEvent.click(resumeButton);
        expect(handleResume).toHaveBeenCalled();
    });

    test('renders nothing when not paused', () => {
        const { container } = render(
            <AlertBanner isPaused={false} />
        );
        expect(container.firstChild).toBeNull();
    });
});
