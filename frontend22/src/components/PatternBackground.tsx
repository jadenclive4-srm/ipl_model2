import React from 'react';

interface PatternBackgroundProps {
  imagePath: string;
  opacity?: number;
  blendMode?: 'normal' | 'multiply' | 'screen' | 'overlay';
}

const PatternBackground: React.FC<PatternBackgroundProps> = ({
  imagePath,
  opacity = 0.1,
  blendMode = 'normal',
}) => {
  return (
    <div
      className="fixed inset-0 pointer-events-none overflow-hidden"
      style={{
        zIndex: 0,
        opacity,
        mixBlendMode: blendMode,
        backgroundImage: `url(${imagePath})`,
        backgroundRepeat: 'repeat',
        backgroundPosition: '0 0',
        backgroundSize: 'auto',
      }}
    />
  );
};

export default PatternBackground;
