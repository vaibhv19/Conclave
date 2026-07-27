import { Client } from '@stomp/stompjs';
import { useAuthStore } from '../store/authStore';
import { useChatStore } from '../store/chatStore';

let stompClient = null;
let currentSubscription = null;
let connectionStateCallback = null;

export const setConnectionStateCallback = (cb) => {
    connectionStateCallback = cb;
};

/**
 * Initializes and connects STOMP client over WebSocket, subscribing to room events.
 * Automatically injects the JWT token inside headers and handles automatic reconnect.
 */
export const connectWebSocket = (roomId) => {
    if (stompClient && stompClient.connected) {
        console.log('WebSocket already connected. Re-subscribing...');
        subscribeToRoom(roomId);
        return;
    }

    const { token } = useAuthStore.getState();
    if (!token) {
        console.error('WebSocket connection failed: Missing authentication token.');
        return;
    }

    stompClient = new Client({
        brokerURL: 'ws://localhost:8080/ws-conclave',
        connectHeaders: {
            Authorization: `Bearer ${token}`
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        debug: (str) => {
            console.log('STOMP Debug:', str);
        },
        onConnect: (frame) => {
            console.log('STOMP Connected successfully:', frame);
            if (connectionStateCallback) connectionStateCallback(true);
            subscribeToRoom(roomId);
        },
        onStompError: (frame) => {
            console.error('STOMP Broker error occurred:', frame.headers['message']);
            console.error('STOMP error details:', frame.body);
            if (connectionStateCallback) connectionStateCallback(false);
        },
        onWebSocketClose: (evt) => {
            console.warn('WebSocket connection closed:', evt);
            if (connectionStateCallback) connectionStateCallback(false);
        }
    });

    stompClient.activate();
};

const subscribeToRoom = (roomId) => {
    if (!stompClient || !stompClient.connected) return;

    if (currentSubscription) {
        console.log('Cleaning up existing room subscription...');
        currentSubscription.unsubscribe();
        currentSubscription = null;
    }

    const destination = `/topic/room/${roomId}`;
    console.log(`Subscribing to destination: ${destination}`);

    currentSubscription = stompClient.subscribe(destination, (message) => {
        try {
            const body = JSON.parse(message.body);
            console.log('WebSocket Event received:', body);

            const chatStore = useChatStore.getState();
            switch (body.type) {
                case 'TURN_STARTED':
                    chatStore.handleTurnStarted(body, roomId);
                    break;
                case 'CONTENT_CHUNK':
                    chatStore.handleContentChunk(body);
                    break;
                case 'TURN_COMPLETED':
                    chatStore.handleTurnCompleted(body, roomId);
                    break;
                case 'SYSTEM_INTERVENTION':
                    chatStore.handleSystemIntervention(body, roomId);
                    break;
                default:
                    console.warn('Unhandled WebSocket event type:', body.type);
            }
        } catch (err) {
            console.error('Failed to parse incoming WebSocket message body:', err);
        }
    });
};

/**
 * Disconnects the active STOMP client and cleans up subscription.
 */
export const disconnectWebSocket = () => {
    if (currentSubscription) {
        currentSubscription.unsubscribe();
        currentSubscription = null;
    }

    if (stompClient) {
        console.log('Disconnecting STOMP client...');
        stompClient.deactivate();
        stompClient = null;
    }
    
    if (connectionStateCallback) connectionStateCallback(false);
};
