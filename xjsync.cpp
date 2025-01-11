/* main.c */

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#ifdef __MACOSX__
#include <unistd.h>
#include <Carbon/Carbon.h>

#include "main.h"
#endif

#ifdef _Windows
#include <direct.h>
#include <windows.h>

#include <winlib/f_type.h>
#include <winlib/winlib.h>

#define chdir( d ) _chdir( d )
#endif

static int xjsyncMain(int argc, char *argv[])
{
	size_t	newLen, actLen;
	int		i;
	char	*command;
	char	*myName = strdup( argv[0] );
	char	*lastSlash = strrchr( myName, '/' );

	if( !lastSlash )
		lastSlash = strrchr( myName, '\\' );

	if( lastSlash )
		*lastSlash = 0;
	if( chdir( myName ) )
		perror( "sync" );

	if( chdir( "../Resources/Java" ) )
		perror( "sync" );
		
#ifdef __MACOSX__
	if( chdir( "/Users/gak/tristan/Object/XJ Sync.app/Contents/Resources/Java" ) )
		perror( "sync" );
#endif
		
#ifdef _Windows
	command = strdup( "javaw  -Xmx1024m  -classpath XJsync.jar XJsyncApp" );
#else
	command = strdup( "java -Xmx1024m -classpath XJsync.jar XJsyncApp" );
#endif
	actLen = strlen( command )+1;

	for( i=1; i<argc; i++ )
	{
		newLen = actLen + strlen(argv[i])+1;
		command = (char *)realloc( command, newLen );
		strcat( command, " " );
		strcat( command, argv[i] );
		actLen = newLen;
	}

#ifdef __MACOSX__
	newLen = actLen + strlen(" &")+1;
	command = (char *)realloc( command, newLen );
	strcat( command, " " );
	strcat( command, " &" );
	actLen = newLen;
#endif
#ifdef _Windows
	WinExec( command, SW_SHOW );
	Sleep( 10000 );
	closeStartup();
	return 0;
#else
	return system( command );
#endif
}

#ifdef _Windows
int PASCAL WinMain( HINSTANCE inst, HINSTANCE, LPSTR command, int )
{
	openStartup( NULL, "SPLASH_SCREEN" );

	winlib::FileTypeRegistry	theType;

	char						theFileName[1024];
	char						*argv[3];

	GetModuleFileName( inst, theFileName, sizeof( theFileName ) -1 );

	theType.type = "XJSyncDB";
	theType.type_description = "XJ Sync Datenbank";
	theType.icon = theFileName;
	theType.icon += ",1";

	theType.cmd = "open";
	theType.cmd_description = "Öffnen";

	theType.commandLine = theFileName;
	theType.commandLine += " \"%1\"";

	theType.extension = ".xsd";
	winlib::addFileType( &theType );

	theType.extension = ".xjSyncDB";
	winlib::addFileType( &theType );

	theType.cmd = "execute";
	theType.cmd_description = "Ausführen";

	theType.commandLine = theFileName;
	theType.commandLine += " -e \"%1\"";

	theType.extension = ".xsd";
	winlib::addFileType( &theType );

	theType.extension = ".xjSyncDB";
	winlib::addFileType( &theType );

	argv[0] = theFileName;
	argv[1] = (char*)command;
	argv[2] = 0;


	return xjsyncMain( 2, argv );
}
#endif

#ifdef __MACOSX__
void Initialize(void);	/* function prototypes */
void EventLoop(void);
void MakeWindow(void);
void MakeMenu(void);
void DoEvent(EventRecord *event);
void DoMenuCommand(long menuResult);
void DoAboutBox(void);
void DrawWindow(WindowRef window);
static OSErr QuitAppleEventHandler(const AppleEvent *appleEvt, AppleEvent* reply, UInt32 refcon);

Boolean		gQuitFlag;	/* global */

int main(int argc, char *argv[])
{
	Initialize();
	MakeWindow();
	MakeMenu();

	xjsyncMain( argc, argv );

	// EventLoop();

	return 0;
}
 
void Initialize()	/* Initialize some managers */
{
	OSErr	err;
        
    InitCursor();

    err = AEInstallEventHandler( kCoreEventClass, kAEQuitApplication, NewAEEventHandlerUPP((AEEventHandlerProcPtr)QuitAppleEventHandler), 0, false );
    if (err != noErr)
        ExitToShell();
}

static OSErr QuitAppleEventHandler( const AppleEvent *appleEvt, AppleEvent* reply, UInt32 refcon )
{
    gQuitFlag =  true;
    
    return noErr;
}

void EventLoop()
{
    Boolean	gotEvent;
    EventRecord	event;
        
    gQuitFlag = false;
	
    do
    {
        gotEvent = WaitNextEvent(everyEvent,&event,32767,nil);
        if (gotEvent)
            DoEvent(&event);
    } while (!gQuitFlag);
    
    ExitToShell();					
}

void MakeWindow()	/* Put up a window */
{
    Rect	wRect;
    WindowRef	myWindow;
    
    SetRect(&wRect,50,50,600,200); /* left, top, right, bottom */
    myWindow = NewCWindow(nil, &wRect, "\pHello", true, zoomNoGrow, (WindowRef) -1, true, 0);
    
    if (myWindow != nil)
        SetPort(GetWindowPort(myWindow));  /* set port to new window */
    else
	ExitToShell();					
}

void MakeMenu()		/* Put up a menu */
{
    Handle	menuBar;
    MenuRef	menu;
    long	response;
    OSErr	err;
	
    menuBar = GetNewMBar(rMenuBar);	/* read menus into menu bar */
    if ( menuBar != nil )
    {
        SetMenuBar(menuBar);	/* install menus */
        // AppendResMenu(GetMenuHandle(mApple), 'DRVR');
        
        err = Gestalt(gestaltMenuMgrAttr, &response);
	if ((err == noErr) && (response & gestaltMenuMgrAquaLayoutMask))
        {
            menu = GetMenuHandle( mFile );
			DeleteMenuItem( menu, iQuit );
            DeleteMenuItem( menu, iQuitSeparator );
        }
        
        DrawMenuBar();
    }
    else
    	ExitToShell();
}

void DoEvent(EventRecord *event)
{
    short	part;
    Boolean	hit;
    char	key;
    Rect	tempRect;
    WindowRef	whichWindow;
        
    switch (event->what) 
    {
        case mouseDown:
            part = FindWindow(event->where, &whichWindow);
            switch (part)
            {
                case inMenuBar:  /* process a moused menu command */
                    DoMenuCommand(MenuSelect(event->where));
                    break;
                    
                case inSysWindow:
                    break;
                
                case inContent:
					if (whichWindow != FrontWindow())
                        SelectWindow(whichWindow);
                    break;
                
                case inDrag:	/* pass screenBits.bounds */
                    GetRegionBounds(GetGrayRgn(), &tempRect);
                    DragWindow(whichWindow, event->where, &tempRect);
                    break;
                    
                case inGrow:
                    break;
                    
                case inGoAway:
                    DisposeWindow(whichWindow);
                    ExitToShell();
                    break;
                    
                case inZoomIn:
                case inZoomOut:
                    hit = TrackBox(whichWindow, event->where, part);
                    if (hit) 
                    {
                        SetPort(GetWindowPort(whichWindow));   // window must be current port
                        EraseRect(GetWindowPortBounds(whichWindow, &tempRect));   // inval/erase because of ZoomWindow bug
                        ZoomWindow(whichWindow, part, true);
                        InvalWindowRect(whichWindow, GetWindowPortBounds(whichWindow, &tempRect));	
                    }
                    break;
                }
                break;
		
                case keyDown:
		case autoKey:
                    key = event->message & charCodeMask;
                    if (event->modifiers & cmdKey)
                        if (event->what == keyDown)
                            DoMenuCommand(MenuKey(key));
		case activateEvt:	       /* if you needed to do something special */
                    break;
                    
                case updateEvt:
			DrawWindow((WindowRef) event->message);
			break;
                        
                case kHighLevelEvent:
			AEProcessAppleEvent( event );
			break;
		
                case diskEvt:
			break;
	}
}

void DoMenuCommand(long menuResult)
{
    short	menuID;		/* the resource ID of the selected menu */
    short	menuItem;	/* the item number of the selected menu */
	
    menuID = HiWord(menuResult);    /* use macros to get item & menu number */
    menuItem = LoWord(menuResult);
	
    switch (menuID) 
    {
        case mApple:
            switch (menuItem) 
            {
                case iAbout:
                    DoAboutBox();
                    break;
                    
                case iQuit:
                    ExitToShell();
                    break;
				
                default:
                    break;
            }
            break;
        
        case mFile:
            break;
		
        case mEdit:
            break;
    }
    HiliteMenu(0);	/* unhighlight what MenuSelect (or MenuKey) hilited */
}

void DrawWindow(WindowRef window)
{
    Rect		tempRect;
    GrafPtr		curPort;
	
    GetPort(&curPort);
    SetPort(GetWindowPort(window));
    BeginUpdate(window);
    EraseRect(GetWindowPortBounds(window, &tempRect));
    DrawControls(window);
    DrawGrowIcon(window);
    EndUpdate(window);
    SetPort(curPort);
}

void DoAboutBox(void)
{
   (void) Alert(kAboutBox, nil);  // simple alert dialog box
}
#endif