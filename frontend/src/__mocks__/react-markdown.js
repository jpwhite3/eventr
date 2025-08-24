import React from 'react';

// Mock react-markdown for Jest tests
const ReactMarkdown = ({ children }) => <div data-testid="react-markdown">{children}</div>;

export default ReactMarkdown;