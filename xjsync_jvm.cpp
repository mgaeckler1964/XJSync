/*
 *  XJSyncjnilib.c
 *  XJSync
 *
 *  Created by Martin GŠckler on 08.03.06.
 *  Copyright (c) 2006 __MyCompanyName__. All rights reserved.
 *
 */

#include <string.h>
#include <stdio.h>

#ifdef _Windows
#include <winlib/f_type.h>
#include <windows.h>
#endif

#ifdef __MACH__
#include <unistd.h>
#endif

#include <jni.h>       /* where everything is defined */

static int xjsyncMain( int argc, const char *argv[] )
{
	jint	ret;
	JavaVM	*jvm;       /* denotes a Java VM */
	JNIEnv	*env;       /* pointer to native method interface */

	JavaVMInitArgs	vm_args;
	JavaVMOption	options[2];

#ifdef __MACH__
	char	*myArchive = "../Resources/Java/XJsync.jar";
#else
	char	*myArchive = "..\\Resources\\Java\\XJsync.jar";
#endif
	char	optionsStr[1024];

	sprintf( optionsStr, "-Djava.class.path=%s", myArchive );

	vm_args.version = JNI_VERSION_1_2;
	options[0].optionString = optionsStr;
#ifdef __MACH__
	vm_args.nOptions = 2;
	options[1].optionString = "-Dapple.laf.useScreenMenuBar=true";
#else
	vm_args.nOptions = 1;
#endif
	vm_args.options = options;
	vm_args.ignoreUnrecognized = JNI_FALSE;

	ret=JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);
	if( ret >= 0 )
	{
		/* invoke the Main.test method using the JNI */ 
		jclass cls = env->FindClass("XJsyncApp"); 
		if( cls )
		{
			jmethodID mid = env->GetStaticMethodID(cls, "main", "([Ljava/lang/String;)V"); 
			if( mid )
			{
				jstring jstr = env->NewStringUTF("hello");
				if(jstr != NULL)
				{
					jclass stringClass = env->FindClass("java/lang/String");
					if( stringClass )
					{
						jobjectArray args = env->NewObjectArray(argc-1, stringClass, jstr);
						if( args )
						{
							for( int i=1; i<argc; i++ )
								env->SetObjectArrayElement( args, i-1, env->NewStringUTF(argv[i]) );

							env->CallStaticVoidMethod(cls, mid, args); 
						}
						else
							puts( "Array nicht erzeugt" );
					}
					else
						puts( "Stringklasse nicht gefunden" );
				}
				else
					puts( "UTF String nicht erzeugt" );
			}
			else
				puts( "Methode nicht gefunden" );
		}
		else
			puts( "Klasse nicht gefunen" );

		/* We are done. */ 
		jvm->DestroyJavaVM();
	}
	else
		printf( "JVM nicht initialisiert: %d\n", (int)ret );

	return 0;
}

#ifdef _Windows
int PASCAL WinMain( HINSTANCE inst, HINSTANCE, LPSTR command, int )
{
	FILE_TYPE	theType;

	char		theFileName[1024];
	const char	*argv[3];

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
	addFileType( &theType );

	theType.extension = ".xjSyncDB";
	addFileType( &theType );

	theType.cmd = "execute";
	theType.cmd_description = "Ausführen";

	theType.commandLine = theFileName;
	theType.commandLine += " -e \"%1\"";

	theType.extension = ".xsd";
	addFileType( &theType );

	theType.extension = ".xjSyncDB";
	addFileType( &theType );

	argv[0] = theFileName;
	argv[1] = (char*)command;
	argv[2] = 0;

	return xjsyncMain( 2, argv );
}
#else
int main( int argc, const char *argv[] )
{
	char	*slashPos;
	char	myPath[1024];
	
	strcpy( myPath, argv[0] );
	slashPos = strrchr( myPath, '/' );
	if( !slashPos )
		slashPos = strrchr( myPath, '\\' );
	if( slashPos )
		*slashPos = 0;
		
	chdir( myPath );
	
	return xjsyncMain( argc, argv );
}
#endif
