const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const crypto = require('crypto');
const chalk = require('chalk');
const moment = require('moment');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3002;

// Store received webhooks in memory for the web interface
const webhookHistory = [];
const MAX_HISTORY = 100;

// Middleware
app.use(helmet());
app.use(cors());
app.use(morgan('combined'));

// Parse raw body for signature verification
app.use('/webhook', express.raw({ type: 'application/json' }));
app.use(express.json()); // For other routes
app.use(express.static(path.join(__dirname, 'public')));

// Utility functions
function verifyWebhookSignature(payload, signature, secret) {
    if (!signature || !secret) {
        return false;
    }
    
    // Remove 'sha256=' prefix if present
    const receivedSignature = signature.startsWith('sha256=') 
        ? signature.slice(7) 
        : signature;
    
    // Compute expected signature
    const expectedSignature = crypto
        .createHmac('sha256', secret)
        .update(payload, 'utf8')
        .digest('hex');
    
    // Use constant-time comparison
    try {
        return crypto.timingSafeEqual(
            Buffer.from(receivedSignature, 'hex'),
            Buffer.from(expectedSignature, 'hex')
        );
    } catch (error) {
        console.error(chalk.red('Signature verification error:'), error.message);
        return false;
    }
}

function logWebhook(webhook, isValid, error = null) {
    const timestamp = moment().format('YYYY-MM-DD HH:mm:ss');
    const eventType = webhook.eventType || 'UNKNOWN';
    const webhookId = webhook.metadata?.webhookId || 'N/A';
    
    console.log('\n' + chalk.blue('='.repeat(60)));
    console.log(chalk.blue.bold(`📨 WEBHOOK RECEIVED - ${timestamp}`));
    console.log(chalk.blue('='.repeat(60)));
    
    console.log(chalk.cyan('Event Type:'), chalk.white.bold(eventType));
    console.log(chalk.cyan('Webhook ID:'), chalk.white(webhookId));
    console.log(chalk.cyan('Event ID:'), chalk.white(webhook.eventId || 'N/A'));
    console.log(chalk.cyan('Timestamp:'), chalk.white(webhook.timestamp || 'N/A'));
    
    if (isValid) {
        console.log(chalk.green('✅ Signature:'), chalk.green.bold('VALID'));
    } else {
        console.log(chalk.red('❌ Signature:'), chalk.red.bold('INVALID'));
    }
    
    if (error) {
        console.log(chalk.red('Error:'), chalk.red(error));
    }
    
    console.log(chalk.cyan('Data:'));
    console.log(JSON.stringify(webhook.data || {}, null, 2));
    
    console.log(chalk.blue('='.repeat(60)) + '\n');
    
    // Store in history for web interface
    const historyEntry = {
        id: Date.now() + Math.random(),
        timestamp,
        eventType,
        webhookId,
        eventId: webhook.eventId || 'N/A',
        isValid,
        error,
        payload: webhook,
        rawPayload: JSON.stringify(webhook, null, 2)
    };
    
    webhookHistory.unshift(historyEntry);
    if (webhookHistory.length > MAX_HISTORY) {
        webhookHistory.pop();
    }
}

// Webhook endpoint
app.post('/webhook', (req, res) => {
    try {
        const signature = req.headers['x-eventr-signature'];
        const payload = req.body;
        const secret = process.env.EVENTR_WEBHOOK_SECRET || 'dev-secret-key';
        
        // Parse JSON payload
        let webhookData;
        try {
            webhookData = JSON.parse(payload.toString());
        } catch (parseError) {
            console.error(chalk.red('Failed to parse webhook payload:'), parseError.message);
            logWebhook({ eventType: 'PARSE_ERROR' }, false, 'Invalid JSON payload');
            return res.status(400).json({ error: 'Invalid JSON payload' });
        }
        
        // Verify signature
        const isValid = verifyWebhookSignature(payload, signature, secret);
        
        // Log the webhook
        logWebhook(webhookData, isValid);
        
        // Respond based on signature validity
        if (isValid) {
            res.status(200).json({ 
                success: true, 
                message: 'Webhook received and verified',
                eventType: webhookData.eventType,
                eventId: webhookData.eventId
            });
        } else {
            res.status(401).json({ 
                error: 'Invalid signature',
                message: 'Webhook signature verification failed'
            });
        }
        
    } catch (error) {
        console.error(chalk.red('Webhook processing error:'), error);
        logWebhook({ eventType: 'ERROR' }, false, error.message);
        res.status(500).json({ error: 'Internal server error' });
    }
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ 
        status: 'healthy',
        timestamp: new Date().toISOString(),
        uptime: process.uptime(),
        webhooksReceived: webhookHistory.length
    });
});

// API endpoint to get webhook history
app.get('/api/webhooks', (req, res) => {
    const limit = parseInt(req.query.limit) || 50;
    const offset = parseInt(req.query.offset) || 0;
    
    const results = webhookHistory.slice(offset, offset + limit);
    
    res.json({
        webhooks: results,
        total: webhookHistory.length,
        limit,
        offset
    });
});

// API endpoint to clear webhook history
app.delete('/api/webhooks', (req, res) => {
    const count = webhookHistory.length;
    webhookHistory.length = 0;
    
    console.log(chalk.yellow(`🧹 Cleared ${count} webhooks from history`));
    
    res.json({ 
        success: true, 
        cleared: count,
        message: `Cleared ${count} webhooks from history`
    });
});

// Serve the web interface
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Start the server
app.listen(PORT, () => {
    console.log('\n' + chalk.green('🚀 Eventr Webhook Test Client Started!'));
    console.log(chalk.green('='.repeat(40)));
    console.log(chalk.cyan('Server:'), chalk.white(`http://localhost:${PORT}`));
    console.log(chalk.cyan('Webhook URL:'), chalk.white(`http://localhost:${PORT}/webhook`));
    console.log(chalk.cyan('Health Check:'), chalk.white(`http://localhost:${PORT}/health`));
    console.log(chalk.cyan('Environment:'));
    console.log(chalk.cyan('  - Secret Key:'), process.env.EVENTR_WEBHOOK_SECRET ? 
        chalk.green('SET') : chalk.yellow('Using default (dev-secret-key)'));
    console.log(chalk.cyan('  - Port:'), chalk.white(PORT));
    console.log(chalk.green('='.repeat(40)));
    console.log(chalk.white('Ready to receive webhooks! 📨\n'));
});

// Graceful shutdown
process.on('SIGTERM', () => {
    console.log(chalk.yellow('\n🛑 Received SIGTERM, shutting down gracefully...'));
    process.exit(0);
});

process.on('SIGINT', () => {
    console.log(chalk.yellow('\n🛑 Received SIGINT, shutting down gracefully...'));
    process.exit(0);
});