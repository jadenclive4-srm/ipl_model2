import React from 'react';

interface ScatteredIcon {
  id: number;
  svg: React.ReactNode;
  top: number;
  left: number;
  rotation: number;
  scale: number;
  opacity: number;
  duration: number;
  delay: number;
}

const cricketIcons = [
  <svg key="bat" viewBox="0 0 94 164" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
    <path d="M46.5 0C20.8 0 0 20.8 0 46.5c0 25.7 20.8 46.5 46.5 46.5 25.7 0 46.5-20.8 46.5-46.5C93 20.8 72.2 0 46.5 0z" opacity="0.3"/>
    <path d="M46.5 10C28.2 10 10 28.2 10 46.5s18.2 36.5 36.5 36.5 36.5-18.2 36.5-36.5S64.8 10 46.5 10z" opacity="0.2"/>
    <path d="M46.5 20L35 50l5 10 15-25 15 25 5-10-11-20z" opacity="0.4"/>
  </svg>,
  <svg key="helmet" viewBox="0 0 126 132" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
    <ellipse cx="63" cy="66" rx="50" ry="55" opacity="0.2"/>
    <ellipse cx="63" cy="60" rx="40" ry="45" opacity="0.25"/>
    <path d="M63 20L40 35l5 25 45-15-5-25z" opacity="0.15"/>
  </svg>,
  <svg key="stumps" viewBox="0 0 93 138" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
    <rect x="30" y="10" width="8" height="120" rx="4" opacity="0.15"/>
    <rect x="45" y="10" width="8" height="120" rx="4" opacity="0.15"/>
    <rect x="60" y="10" width="8" height="120" rx="4" opacity="0.15"/>
    <path d="M25 120h43" stroke="currentColor" strokeWidth="6" strokeLinecap="round" opacity="0.2"/>
  </svg>,
  <svg key="ball" viewBox="0 0 100 100" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
    <circle cx="50" cy="50" r="45" opacity="0.12"/>
    <circle cx="50" cy="50" r="35" opacity="0.15"/>
  </svg>,
  <svg key="cap" viewBox="0 0 126 90" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
    <path d="M20 50c0-15 5-25 25-25s25 10 25 25-10 30-25 30-25-15-25-30z" opacity="0.15"/>
    <path d="M10 55h106" stroke="currentColor" strokeWidth="4" strokeLinecap="round" opacity="0.2"/>
  </svg>,
  <svg key="batsman" viewBox="0 0 121 151" fill="currentColor" xmlns="http://www.w3.org/2000/svg">
    <circle cx="60" cy="35" r="20" opacity="0.15"/>
    <path d="M60 55L40 75h30l-10 50h20l-10 50-30-20-30 20-10-50h20l-10-50h30z" opacity="0.1"/>
  </svg>,
];

const generateRandomIcon = (): React.ReactNode => {
  return cricketIcons[Math.floor(Math.random() * cricketIcons.length)];
};

const generateRandomPosition = (max: number): number => {
  return Math.random() * max;
};

const generateRandomRotation = (): number => {
  return (Math.random() - 0.5) * 60;
};

const generateRandomScale = (): number => {
  return 0.4 + Math.random() * 1.2;
};

const generateRandomOpacity = (): number => {
  return 0.04 + Math.random() * 0.12;
};

const generateRandomDuration = (): number => {
  return 15 + Math.random() * 20;
};

const generateRandomDelay = (): number => {
  return Math.random() * -20;
};

const ScatteredBackground: React.FC = () => {
  const iconCount = 25;
  const icons: ScatteredIcon[] = [];

  for (let i = 0; i < iconCount; i++) {
    icons.push({
      id: i,
      svg: generateRandomIcon(),
      top: generateRandomPosition(100),
      left: generateRandomPosition(100),
      rotation: generateRandomRotation(),
      scale: generateRandomScale(),
      opacity: generateRandomOpacity(),
      duration: generateRandomDuration(),
      delay: generateRandomDelay(),
    });
  }

  return (
    <div className="fixed inset-0 pointer-events-none overflow-hidden" style={{ zIndex: 0 }}>
      <style>{`
        @keyframes float {
          0% {
            transform: translateY(0px) rotate(var(--rotation)) scale(var(--scale));
          }
          100% {
            transform: translateY(-20px) rotate(calc(var(--rotation) + 5deg)) scale(var(--scale));
          }
        }
      `}</style>
      {icons.map((icon) => (
        <div
          key={icon.id}
          className="absolute text-spotify-green will-change-transform"
          style={{
            '--rotation': `${icon.rotation}deg`,
            '--scale': icon.scale,
            top: `${icon.top}%`,
            left: `${icon.left}%`,
            opacity: icon.opacity,
            width: `${50 + Math.random() * 50}px`,
            height: `${50 + Math.random() * 50}px`,
            animation: `float ${icon.duration}s ease-in-out ${icon.delay}s infinite alternate`,
          } as React.CSSProperties}
        >
          {React.cloneElement(icon.svg as React.ReactElement, {
            style: { width: '100%', height: '100%' },
          })}
        </div>
      ))}
    </div>
  );
};

export default ScatteredBackground;
