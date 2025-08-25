declare module '@fortawesome/react-fontawesome' {
  import { IconDefinition } from '@fortawesome/fontawesome-svg-core';
  import React from 'react';

  export interface FontAwesomeIconProps {
    icon: IconDefinition;
    className?: string;
    size?: string;
    color?: string;
    spin?: boolean;
    pulse?: boolean;
    border?: boolean;
    fixedWidth?: boolean;
    inverse?: boolean;
    listItem?: boolean;
    flip?: 'horizontal' | 'vertical' | 'both';
    rotation?: 90 | 180 | 270;
    pull?: 'left' | 'right';
    style?: React.CSSProperties;
  }

  export const FontAwesomeIcon: React.FC<FontAwesomeIconProps>;
}