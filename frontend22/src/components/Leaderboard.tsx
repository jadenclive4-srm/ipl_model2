// frontend22/src/components/Leaderboard.tsx

import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';
import { User } from '../types/api';

interface LeaderboardProps {
  isAdmin?: boolean;
}

type TeamsMode = 'webhook' | 'groupchat' | 'graph' | 'powerautomate';

const Leaderboard: React.FC<LeaderboardProps> = ({ isAdmin = false }) => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
   const [sendingToTeams, setSendingToTeams] = useState(false);
   const [teamsMessage, setTeamsMessage] = useState<{ success: boolean; message: string } | null>(null);
   const [deepLink, setDeepLink] = useState<string | null>(null);
   const [openingTeams, setOpeningTeams] = useState(false);
   const [copied, setCopied] = useState(false);
   const [teamsGroupChatLink, setTeamsGroupChatLink] = useState<string>(
     localStorage.getItem('teamsGroupChatLink') || ''
   );
   const [showLinkInput, setShowLinkInput] = useState(false);
  
  // Teams selection state
  const [teamsMode, setTeamsMode] = useState<TeamsMode>('webhook');
  const [availableChannels, setAvailableChannels] = useState<string[]>(['default', 'all']);
  const [availableGroupChats, setAvailableGroupChats] = useState<string[]>(['default']);
  const [selectedTarget, setSelectedTarget] = useState<string>('default');
  const [showModeDropdown, setShowModeDropdown] = useState(false);
  const [showTargetDropdown, setShowTargetDropdown] = useState(false);

  useEffect(() => {
    loadLeaderboard();
    
    // Refresh every 10 seconds
    const interval = setInterval(loadLeaderboard, 10000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (isAdmin) {
      loadAvailableTargets();
    }
  }, [isAdmin]);

  const loadLeaderboard = async () => {
    try {
      setLoading(true);
      const data = await apiService.getLeaderboard();
      setUsers(data);
    } catch (error) {
      setError(error instanceof Error ? error.message : 'Failed to load leaderboard');
    } finally {
      setLoading(false);
    }
  };

  const loadAvailableTargets = async () => {
    try {
      const [channelsRes, chatsRes] = await Promise.all([
        apiService.getAvailableTeamsChannels().catch(() => ({ channels: [] as string[], count: 0 })),
        apiService.getAvailableGroupChats().catch(() => ({ groupChats: [] as string[], count: 0 }))
      ]);
      
      const channels = channelsRes.channels || [];
      const chats = chatsRes.groupChats || [];
      
      setAvailableChannels(['default', 'all', ...channels]);
      setAvailableGroupChats(['default', ...chats]);
      
      setSelectedTarget(channels.includes('default') ? 'default' : (channels[0] || 'default'));
    } catch (error) {
      console.error('Failed to load Teams targets:', error);
      setAvailableChannels(['default', 'all']);
      setAvailableGroupChats(['default']);
      setSelectedTarget('default');
    }
  };

   const handleSendToTeams = async () => {
     try {
       setSendingToTeams(true);
       setTeamsMessage(null);

       let response;
       if (teamsMode === 'powerautomate') {
         response = await apiService.sendViaPowerAutomate();
       } else if (teamsMode === 'groupchat' || teamsMode === 'graph') {
         response = await apiService.sendTopToGroupChat(selectedTarget, teamsMode);
       } else {
         response = await apiService.sendTopToTeams(selectedTarget, teamsMode);
       }

       setTeamsMessage({
         success: response.success,
         message: response.message
       });
     } catch (error) {
       setTeamsMessage({
         success: false,
         message: error instanceof Error ? error.message : 'Failed to send to Teams'
       });
     } finally {
       setSendingToTeams(false);
      }
    };

    const handleCopyTop5 = async () => {
     try {
       const top5 = users.slice(0, 5);
       if (top5.length === 0) {
         setTeamsMessage({ success: false, message: 'No leaderboard data to copy' });
         return;
       }

       // Build formatted text
       let text = '🏆 IPL Top 5\n\n';

       top5.forEach((user, index) => {
         const rank = index + 1;
         const name = user.fullName || user.username;
         const points = user.points;

         if (rank === 1) {
           text += `🥇 ${name} - ${points} points\n`;
         } else if (rank === 2) {
           text += `🥈 ${name} - ${points} points\n`;
         } else if (rank === 3) {
           text += `🥉 ${name} - ${points} points\n`;
         } else {
           text += `   ${name} - ${points} points\n`;
         }
       });

       // Remove trailing newline
       text = text.trim();

       // Copy to clipboard
       await navigator.clipboard.writeText(text);

       // Show feedback
       setCopied(true);
       setTimeout(() => setCopied(false), 2000);

       setTeamsMessage({ success: true, message: 'Top 5 copied to clipboard!' });
     } catch (error) {
       setTeamsMessage({
         success: false,
         message: 'Failed to copy to clipboard'
       });
     }
   };

   const handleSaveTeamsLink = () => {
     localStorage.setItem('teamsGroupChatLink', teamsGroupChatLink);
     setShowLinkInput(false);
     setTeamsMessage({ success: true, message: 'Teams group chat link saved!' });
     setTimeout(() => setTeamsMessage(null), 3000);
   };

   const extractChatId = (link: string): string | null => {
     try {
       const url = new URL(link);
       // Path format: /l/chat/19:CHAT_ID@thread.v2/conversations
       const pathParts = url.pathname.split('/');
       if (pathParts.length >= 4 && pathParts[2] === 'chat') {
         const chatId = pathParts[3];
         if (chatId && chatId.includes('@thread.v2')) {
           console.log('Extracted chat ID:', chatId);
           return chatId;
         }
       }
       console.warn('Could not extract chat ID from link:', link);
       return null;
     } catch (error) {
       console.error('Error parsing Teams link:', error);
       return null;
     }
   };

   const handleOpenInTeams = async () => {
     try {
       setOpeningTeams(true);
       setTeamsMessage(null);

       // Use the full stored chat link directly
       if (!teamsGroupChatLink || !teamsGroupChatLink.includes('teams.microsoft.com/l/chat/')) {
         setTeamsMessage({
           success: false,
           message: 'Invalid Teams group chat link. Please set a valid link first.'
         });
         setOpeningTeams(false);
         return;
       }

       // Get the deep link - backend will just append message to this URL
       const response = await apiService.getTeamsDeepLink(teamsGroupChatLink);

       if (response.success && response.deepLink) {
         setDeepLink(response.deepLink);
         // Navigate to the deep link
         window.location.href = response.deepLink;
       } else {
         setTeamsMessage({
           success: false,
           message: response.message || 'Failed to generate Teams link'
         });
       }
     } catch (error) {
       setTeamsMessage({
         success: false,
         message: error instanceof Error ? error.message : 'Failed to open Teams'
       });
     } finally {
       setOpeningTeams(false);
     }
   };

  const getModeLabel = () => {
    switch (teamsMode) {
      case 'webhook': return '📺 Channel';
      case 'powerautomate': return '⚡ Power Automate';
      default: return '💬 Group Chat';
    }
  };

  const getTargetLabel = () => {
    if (teamsMode === 'powerautomate') return '⚡ Power Automate';
    if (selectedTarget === 'all' && teamsMode === 'webhook') return '📢 All Channels';
    if (selectedTarget === 'default') {
      return teamsMode === 'webhook' ? '💬 Default Channel' : '💬 Default Group Chat';
    }
    return `💬 ${selectedTarget}`;
  };

  const getSendButtonTitle = () => {
    if (teamsMode === 'powerautomate') return 'Send top 5 via Power Automate flow';
    return `Send top 5 to ${teamsMode === 'webhook' ? selectedTarget + ' channel' : selectedTarget + ' group chat'}`;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <div className="text-lg text-spotify-textSecondary">Loading leaderboard...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-900 border border-red-500 text-red-200 px-4 py-3 rounded mb-4">
        {error}
      </div>
    );
  }

  return (
    <div className="bg-spotify-surface border border-spotify-surfaceLight shadow-lg overflow-hidden sm:rounded-lg">
      <div className="px-4 py-5 sm:px-6 border-b border-spotify-surfaceLight">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h3 className="text-lg leading-6 font-medium text-spotify-green">Leaderboard</h3>
            <p className="mt-1 max-w-2xl text-sm text-spotify-textSecondary">
              Top performers in IPL predictions
            </p>
          </div>
          
           {isAdmin && (
             <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
               {/* Teams Group Chat Link Setting */}
               {!showLinkInput ? (
                 <button
                   type="button"
                   onClick={() => setShowLinkInput(true)}
                   className="inline-flex items-center px-3 py-2 border border-spotify-surfaceLight bg-spotify-surface text-sm font-medium rounded-md text-spotify-text hover:bg-spotify-surfaceLight focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-spotify-green"
                   title="Set Teams group chat link"
                 >
                   <svg className="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 24 24">
                     <path d="M19.14 12.94c.04-.31.06-.63.06-.94 0-.31-.02-.63-.06-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.04.31-.06.63-.06.94s.02.63.06.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/>
                   </svg>
                   {teamsGroupChatLink ? 'Change Teams Chat Link' : 'Set Teams Chat Link'}
                 </button>
               ) : (
                 <div className="flex items-center gap-2">
                   <input
                     type="text"
                     value={teamsGroupChatLink}
                     onChange={(e) => setTeamsGroupChatLink(e.target.value)}
                     placeholder="Paste Teams group chat link here"
                     className="px-3 py-2 border border-spotify-surfaceLight bg-spotify-surface text-sm font-medium rounded-md text-spotify-text placeholder-spotify-textMuted focus:outline-none focus:ring-2 focus:ring-spotify-green w-64"
                   />
                   <button
                     type="button"
                     onClick={handleSaveTeamsLink}
                     className="inline-flex items-center px-3 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
                   >
                     Save
                   </button>
                   <button
                     type="button"
                     onClick={() => {
                       setShowLinkInput(false);
                       setTeamsGroupChatLink(localStorage.getItem('teamsGroupChatLink') || '');
                     }}
                     className="inline-flex items-center px-3 py-2 border border-spotify-surfaceLight bg-spotify-surface text-sm font-medium rounded-md text-spotify-text hover:bg-spotify-surfaceLight focus:outline-none"
                   >
                     Cancel
                   </button>
                 </div>
               )}

               {/* Mode Selector */}
              <div className="relative">
                <button
                  type="button"
                  onClick={() => setShowModeDropdown(!showModeDropdown)}
                  className="inline-flex items-center px-3 py-2 border border-spotify-surfaceLight bg-spotify-surface text-sm font-medium rounded-md text-spotify-text hover:bg-spotify-surfaceLight focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-spotify-green"
                  title="Select Teams destination type"
                >
                  <span className="mr-2">{getModeLabel()}</span>
                  <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
                  </svg>
                </button>
                
                {showModeDropdown && (
                  <>
                    <div className="fixed inset-0 z-10" onClick={() => setShowModeDropdown(false)}></div>
                    <div className="absolute right-0 mt-2 w-48 bg-spotify-surface border border-spotify-surfaceLight rounded-md shadow-lg z-20">
                      <div className="py-1">
                        <button
                          onClick={() => {
                            setTeamsMode('webhook');
                            setSelectedTarget('default');
                            setShowModeDropdown(false);
                          }}
                          className={`w-full text-left px-4 py-2 text-sm hover:bg-spotify-surfaceLight transition-colors ${
                            teamsMode === 'webhook' ? 'text-spotify-green bg-spotify-surfaceLight' : 'text-spotify-text'
                          }`}
                        >
                          📺 Channel (Webhook)
                        </button>
                        <button
                          onClick={() => {
                            setTeamsMode('groupchat');
                            setSelectedTarget('default');
                            setShowModeDropdown(false);
                          }}
                          className={`w-full text-left px-4 py-2 text-sm hover:bg-spotify-surfaceLight transition-colors ${
                            teamsMode === 'groupchat' ? 'text-spotify-green bg-spotify-surfaceLight' : 'text-spotify-text'
                          }`}
                        >
                          💬 Group Chat (Graph API)
                        </button>
                        <button
                          onClick={() => {
                            setTeamsMode('powerautomate');
                            setSelectedTarget('default');
                            setShowModeDropdown(false);
                          }}
                          className={`w-full text-left px-4 py-2 text-sm hover:bg-spotify-surfaceLight transition-colors ${
                            teamsMode === 'powerautomate' ? 'text-spotify-green bg-spotify-surfaceLight' : 'text-spotify-text'
                          }`}
                        >
                          ⚡ Power Automate (Easy)
                        </button>
                      </div>
                    </div>
                  </>
                )}
              </div>

              {/* Target Selector - Hidden for Power Automate */}
              {teamsMode !== 'powerautomate' && (
                <div className="relative">
                  <button
                    type="button"
                    onClick={() => setShowTargetDropdown(!showTargetDropdown)}
                    className="inline-flex items-center px-3 py-2 border border-spotify-surfaceLight bg-spotify-surface text-sm font-medium rounded-md text-spotify-text hover:bg-spotify-surfaceLight focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-spotify-green"
                    title={`Select ${teamsMode === 'webhook' ? 'channel' : 'group chat'} to send to`}
                  >
                    <span className="mr-2">{getTargetLabel()}</span>
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
                    </svg>
                  </button>
                  
                  {showTargetDropdown && (
                    <>
                      <div className="fixed inset-0 z-10" onClick={() => setShowTargetDropdown(false)}></div>
                      <div className="absolute right-0 mt-2 w-56 bg-spotify-surface border border-spotify-surfaceLight rounded-md shadow-lg z-20 max-h-60 overflow-y-auto">
                        <div className="py-1">
                          {(teamsMode === 'webhook' ? availableChannels : availableGroupChats)
                            .filter((v, i, a) => a.indexOf(v) === i)
                            .map((target) => (
                            <button
                              key={target}
                              onClick={() => {
                                setSelectedTarget(target);
                                setShowTargetDropdown(false);
                              }}
                              className={`w-full text-left px-4 py-2 text-sm hover:bg-spotify-surfaceLight transition-colors ${
                                selectedTarget === target ? 'text-spotify-green bg-spotify-surfaceLight' : 'text-spotify-text'
                              }`}
                            >
                              {target === 'all' && teamsMode === 'webhook' ? '📢 All Channels' : 
                               target === 'default' ? 
                                 (teamsMode === 'webhook' ? '💬 Default Channel' : '💬 Default Group Chat') : 
                               `💬 ${target}`}
                            </button>
                          ))}
                        </div>
                      </div>
                    </>
                  )}
                </div>
              )}
              
               {/* Send Button */}
               <button
                 onClick={handleSendToTeams}
                 disabled={sendingToTeams || users.length === 0}
                 className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-[#6264A7] hover:bg-[#4F51D4] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#6264A7] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                 title={getSendButtonTitle()}
               >
                 {sendingToTeams ? (
                   <>
                     <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                       <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                       <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                     </svg>
                     Sending...
                   </>
                 ) : (
                   <>
                     <svg className="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                       <path d="M19.5 13.5L15.5 9.5L13.5 11.5L17.5 15.5L4.5 18.5L2.5 14.5L8.5 10.5L5.5 7.5L6.5 5.5L10.5 7.5L16.5 5.5L18.5 3.5L20.5 7.5L21.5 9.5L19.5 13.5Z"/>
                     </svg>
                     Send to Teams
                   </>
                 )}
               </button>

               {/* Open in Teams Button (Deep Link) */}
               <button
                 onClick={handleOpenInTeams}
                 disabled={openingTeams || users.length === 0}
                 className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-[#6264A7] hover:bg-[#4F51D4] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-[#6264A7] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                 title="Open Teams with pre-filled message (you click Send)"
               >
                 {openingTeams ? (
                   <>
                     <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                       <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                       <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                     </svg>
                     Opening...
                   </>
                 ) : (
                   <>
                     <svg className="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                       <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/>
                     </svg>
                     Open in Teams
                   </>
                 )}
               </button>

               {/* Copy Top 5 Button */}
               <button
                 onClick={handleCopyTop5}
                 disabled={users.length === 0}
                 className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                 title="Copy Top 5 leaderboard to clipboard"
               >
                 {copied ? (
                   <>
                     <svg className="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
                       <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd"/>
                     </svg>
                     Copied!
                   </>
                 ) : (
                   <>
                     <svg className="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                       <path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/>
                     </svg>
                     Copy Top 5
                   </>
                 )}
               </button>
            </div>
          )}
        </div>
        
         {teamsMessage && (
           <div className={`mt-3 p-3 rounded ${teamsMessage.success ? 'bg-green-900 border border-green-500 text-green-200' : 'bg-red-900 border border-red-500 text-red-200'}`}>
             {teamsMessage.message}
           </div>
         )}

         {/* Teams link status indicator */}
         {isAdmin && teamsGroupChatLink && !showLinkInput && (
           <div className="mt-2 text-xs text-spotify-textSecondary flex items-center">
             <svg className="w-3 h-3 mr-1 text-green-500" fill="currentColor" viewBox="0 0 20 20">
               <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd"/>
             </svg>
             Teams chat link configured: {new URL(teamsGroupChatLink).hostname}
           </div>
         )}
      </div>
      <ul className="divide-y divide-spotify-surfaceLight">
        {users.map((user, index) => (
          <li key={user.id} className="px-4 py-4 sm:px-6 hover:bg-spotify-surfaceLight transition-colors border-b border-spotify-surfaceLight last:border-b-0">
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <span className={`inline-flex items-center justify-center h-8 w-8 rounded-full text-sm font-medium ${
                    index === 0 ? 'bg-yellow-500 text-black' :
                    index === 1 ? 'bg-gray-400 text-black' :
                    index === 2 ? 'bg-orange-500 text-black' :
                    'bg-spotify-surfaceLight text-spotify-text'
                  }`}>
                    #{index + 1}
                  </span>
                </div>
                <div className="ml-4">
                  <div className="text-sm font-medium text-spotify-text">
                    {user.fullName || user.username}
                  </div>
                  <div className="text-sm text-spotify-textMuted">
                    @{user.username}
                  </div>
                </div>
              </div>
              <div className="text-right">
                <div className="text-sm font-medium text-spotify-text">
                  {user.points} points
                </div>
                <div className="text-sm text-spotify-textMuted">
                  Rank: {user.rank}
                </div>
              </div>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default Leaderboard;
