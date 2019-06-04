/*
 BSD 3-Clause License
 
 Copyright (c) 2019, Udaybhaskar Sarma Seetamraju
 All rights reserved.
 
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 
 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.
 
 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.
 
 * Neither the name of the copyright holder nor the names of its
 contributors may be used to endorse or promote products derived from
 this software without specific prior written permission.
 
 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ASUX.yaml;

import org.ASUX.common.Tuple;
import org.ASUX.common.Debug;

import org.ASUX.yaml.MemoryAndContext;
import org.ASUX.yaml.BatchFileGrammer;
import org.ASUX.yaml.Macros;
import org.ASUX.yaml.CmdLineArgs;
import org.ASUX.yaml.CmdLineArgsBasic;
import org.ASUX.yaml.CmdLineArgsBatchCmd;

import java.util.regex.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import java.util.Properties;
import java.util.Set;

/**
 *  <p>This concrete class is part of a set of 4 concrete sub-classes (representing YAML-COMMANDS to read/query, list, delete and replace ).</p>
 *  <p>This class contains implementation batch-processing of multiple YAML commands (combinations of read, list, delete, replace, macro commands)</p>
 *  <p>This org.ASUX.yaml GitHub.com project and the <a href="https://github.com/org-asux/org.ASUX.cmdline">org.ASUX.cmdline</a> GitHub.com projects.</p>
 *  <p>See full details of how to use this, in {@link org.ASUX.yaml.CmdInvoker} as well as the <a href="https://github.com/org-asux/org-ASUX.github.io/wiki">org.ASUX.cmdline</a> GitHub.com projects.</p>
 *  @param T either the SnakeYaml library's org.yaml.snakeyaml.nodes.Node .. or.. EsotericSoftware Library's preference for LinkedHashMap&lt;String,Object&gt;
 *  @see org.ASUX.YAML.NodeImpl.CmdInvoker
 */
public abstract class BatchCmdProcessor<T extends Object> {

    public static final String CLASSNAME = BatchCmdProcessor.class.getName();

    public static final String FOREACH_INDEX = "foreach.index"; // which iteration # (Int) are we in within the loop.
    public static final String FOREACH_ITER_KEY = "foreach.iteration.key"; // if 'foreach' ends up iterating over an array of strings, then you can get each string's value this way.
    public static final String FOREACH_ITER_VALUE = "foreach.iteration.value"; // if 'foreach' ends up iterating over an array of strings, then you can get each string's value this way.

    // I prefer a LinkedHashMap over a plain HashMap.. as it can help with future enhancements like Properties#1, #2, ..
    // That is being aware of Sequence in which Property-files are loaded.   Can't do that with HashMap
    protected LinkedHashMap<String,Properties> allProps = new LinkedHashMap<String,Properties>();


    /** <p>Whether you want deluge of debug-output onto System.out.</p><p>Set this via the constructor.</p>
     *  <p>It's read-only (final data-attribute).</p>
     */
    protected boolean verbose;

    /** <p>Whether you want a final SHORT SUMMARY onto System.out.</p><p>a summary of how many matches happened, or how many entries were affected or even a short listing of those affected entries.</p>
     */
    public final boolean showStats;

    /**
     * @see org.ASUX.yaml.Enums
     */
    public Enums.ScalarStyle quoteType;

    protected int runcount = 0;
    protected java.util.Date startTime = null;
    protected java.util.Date endTime = null;

    protected MemoryAndContext memoryAndContext = null;

    //==============================================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //==============================================================================
    /** <p>The only constructor - public/private/protected</p>
     *  @param _verbose Whether you want deluge of debug-output onto System.out.
     *  @param _showStats Whether you want a final summary onto console / System.out
     *  @param _quoteType one the values as defined in {@link org.ASUX.Enums} Enummeration
     */
    public BatchCmdProcessor( final boolean _verbose, final boolean _showStats, final Enums.ScalarStyle _quoteType ) {
        this.verbose = _verbose;
        this.showStats = _showStats;
        this.quoteType = _quoteType;

        this.allProps.put( BatchFileGrammer.FOREACH_PROPERTIES, new Properties() );
        this.allProps.put( BatchFileGrammer.GLOBALVARIABLES, new Properties() );
        this.allProps.put( BatchFileGrammer.SYSTEM_ENV, System.getProperties() );
        if ( this.verbose ) new Debug(this.verbose).printAllProps(" >>> ", this.allProps);
    }

    // private BatchCmdProcessor() { this.verbose = false;    this.showStats = true; } // Do Not use this.

    //==============================================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //==============================================================================

    public void setMemoryAndContext( final MemoryAndContext _mnc ) {
        this.memoryAndContext = _mnc;
    }

    //------------------------------------------------------------------------------
    public static class BatchFileException extends Exception {
        public static final long serialVersionUID = 391L;
        public BatchFileException(String _s) { super(_s); }
    }

    //==============================================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //==============================================================================

    /**
     * Because this class is a Generic<T>, compiler (for good reason) will Not allow me to type 'o instanceof T'.  Hence I am delegating this simple condition-check to the sub-classes.
     * @return true if 'o instanceof T' else false.
     */
    protected abstract boolean instanceof_YAMLImplClass( Object o );

    /**
     *  For SnakeYAML Library based subclass of this, simply return 'NodeTools.Node2YAMLString(tempOutput)'.. or .. for EsotericSoftware.com-based LinkedHashMap-based library, simply return 'tools.Map2YAMLString(tempOutputMap)'
     *  @param _node (nullable) either the SnakeYaml library's org.yaml.snakeyaml.nodes.Node ( as generated by SnakeYAML library).. or.. EsotericSoftware Library's preference for LinkedHashMap&lt;String,Object&gt;, -- in either case, this object contains the entire Tree representing the YAML file.
     *  @return a Non-Null String or throws an exception
     *  @throws Exception Any issue whatsoever when dealing with convering YAML/JSON content into Strings
     */
    protected abstract String toStringDebug( T _node ) throws Exception;

    /**
     *  For SnakeYAML Library based subclass of this, simply return 'NodeTools.getEmptyYAML( this.dumperoptions )' .. or .. for EsotericSoftware.com-based LinkedHashMap-based library, simply return 'new LinkedHashMap<>()'
     *  @return either the SnakeYaml library's org.yaml.snakeyaml.nodes.Node ( as generated by SnakeYAML library).. or.. EsotericSoftware Library's preference for LinkedHashMap&lt;String,Object&gt;, -- in either case, this object contains the entire Tree representing the YAML file.
     */
    protected abstract T getEmptyYAML();

    /**
     *  For SnakeYAML Library based subclass of this, simply return 'NodeTools.getNewSingleMap( newRootElem, "", this.dumperoptions )' .. or .. for EsotericSoftware.com-based LinkedHashMap-based library, simply return 'new LinkedHashMap<>.put( newRootElem, "" )'
     *  @param _newRootElemStr the string representing 'lhs' in "lhs: rhs" single YAML entry
     *  @param _valElemStr the string representing 'rhs' in "lhs: rhs" single YAML entry
     *  @return either the SnakeYaml library's org.yaml.snakeyaml.nodes.Node ( as generated by SnakeYAML library).. or.. EsotericSoftware Library's preference for LinkedHashMap&lt;String,Object&gt;, -- in either case, this object contains the entire Tree representing the YAML file.
     */
    protected abstract T getNewSingleYAMLEntry( final String _newRootElemStr, final String _valElemStr );

    /**
     * For SnakeYAML-based subclass of this, simply return 'NodeTools.deepClone( _node )' .. or .. for EsotericSoftware.com-based LinkedHashMap-based library, return ''
     * @param _node A Not-Null instance of either the SnakeYaml library's org.yaml.snakeyaml.nodes.Node ( as generated by SnakeYAML library).. or.. EsotericSoftware Library's preference for LinkedHashMap&lt;String,Object&gt;, -- in either case, this object contains the entire Tree representing the YAML file.
     * @return full deep-clone (Not-Null)
     *  @throws Exception Any issue whatsoever when dealing with cloning, incl. Streamable amd other errors
     */
    protected abstract T deepClone( T _node ) throws Exception;

    //==============================================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //==============================================================================
    /** This is the entry point for this class, with the appropriate TRY-CATCH taken care of, hiding the innumerable exception types.
     *  @param _batchFileName batchfile full path (ry to avoid relative paths)
     *  @param _node either the SnakeYaml library's org.yaml.snakeyaml.nodes.Node ( as generated by SnakeYAML library).. or.. EsotericSoftware Library's preference for LinkedHashMap&lt;String,Object&gt;, -- in either case, this object contains the entire Tree representing the YAML file.
     *  @return a BLANK/EMPTY/NON-NULL org.yaml.snakeyaml.nodes.Node object, as generated by SnakeYAML library and you'll get the final Map output representing all processing done by the batch file
     *  @throws Exception any exception while processing the command(s) within the batchfile
     */
    public T go( final String _batchFileName, final T _node ) throws Exception
    {
        if ( _batchFileName == null || _node == null )
            return getEmptyYAML();  // null (BatchFile) is treated as  batchfile with ZERO commands.
        final String HDR = CLASSNAME +": go(_batchFileName="+ _batchFileName +","+ _node.getClass().getName() +"): ";

        this.startTime = new java.util.Date();
        String line = null;

        final BatchFileGrammer batchCmds = new BatchFileGrammer( this.verbose );

        try {
            if ( batchCmds.openFile( _batchFileName, true, true ) ) {
                if ( this.verbose ) System.out.println( CLASSNAME + ": go(): successfully opened _batchFileName [" + _batchFileName +"]" );
                if ( this.showStats ) System.out.println( _batchFileName +" has "+ batchCmds.getCommandCount() );

                final T  retNode = this.processBatch( false, batchCmds, _node );
                if ( this.verbose ) System.out.println( CLASSNAME +" go():  retNode =" + retNode +"\n\n");

                this.endTime = new java.util.Date();
                if ( this.showStats ) System.out.println( "Ran "+ this.runcount +" commands from "+ this.startTime +" until "+ this.endTime +" = " + (this.endTime.getTime() - this.startTime.getTime()) +" seconds" );
                return retNode;

            } else { // if-else openFile()
                return getEmptyYAML();
            }

        } catch (BatchFileException bfe) {
            if ( this.verbose ) bfe.printStackTrace(System.err);
            System.err.println( "Error while processing: "+ batchCmds.getState() + "\n" + bfe.getMessage() );
        } catch(java.io.FileNotFoundException fe) {
            if ( this.verbose ) fe.printStackTrace(System.err);
            System.err.println( "ERROR In "+ batchCmds.getState() +".\nSee full-details by re-running command using --verbose cmdline option. " );
        } catch (Exception e) {
            if ( this.verbose ) e.printStackTrace(System.err);
            System.err.println( "Unexpected Serious Internal ERROR while processing "+ batchCmds.getState() +".\nSee full-details by re-running command using --verbose cmdline option.");
        }

        return getEmptyYAML();
    }

    //=======================================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //=======================================================================

    /**
     *  This function is meant for recursion.  Recursion happens when 'foreach' or 'batch' commands are detected in a batch file.
     *  After this function completes processing SUCCESSFULLY.. it returns a java.utils.LinkedHashMap&lt;String, Object&gt; object.
     *  If there is any failure whatsoever then the batch-file processing stops immediately.
     *  If there is any failure whatsoever either return value is NULL or an Exception is thrown.
     *  @param _bInRecursion true or false, whether this invocation is a recursive call or not.  If true, when the 'end' or <EOF> is detected.. this function returns
     *  @param _batchCmds an object of type BatchFileGrammer created by reading a batch-file, or .. .. the contents between 'foreach' and 'end' commands
     *  @param _input either the SnakeYaml library's org.yaml.snakeyaml.nodes.Node ( as generated by SnakeYAML library).. or.. EsotericSoftware Library's preference for LinkedHashMap&lt;String,Object&gt;, -- in either case, this object contains the entire Tree representing the YAML file.
     *  @return a BLANK/EMPTY/NON-NULL org.yaml.snakeyaml.nodes.Node object, as generated by SnakeYAML/CollectionsImpl library and you'll get the final YAML output representing all processing done by the batch file.  If there is any failure, either an Exception is thrown.
     *  @throws BatchFileException if any failure trying to execute any entry in the batch file.  Batch file processing will Not proceed once a problem occurs.
     *  @throws FileNotFoundException if the batch file to be loaded does Not exist
     *  @throws Exception
     */
    protected T processBatch( final boolean _bInRecursion, final BatchFileGrammer _batchCmds, T _input )
                        throws BatchFileException, java.io.FileNotFoundException, Exception
    {
        final String HDR = CLASSNAME +": processBatch(recursion="+ _bInRecursion +","+ _batchCmds.getCmdType() +"): ";
        T tempOutput = null; // it's immediately re-initialized within WHILE-Loop below.

        final Properties globalVariables = this.allProps.get( BatchFileGrammer.GLOBALVARIABLES );

        if ( this.verbose ) System.out.println( HDR +" BEFORE STARTING while-loop.. "+ _batchCmds.hasNextLine() +" re: "+ _batchCmds.getState() +" @ BEGINNING _input="+ _input +"]" );

        while ( _batchCmds.hasNextLine() )
        {
            _batchCmds.nextLine(); // we can always get the return value of this statement .. via _batchCmds.getCurrentLine()
            _batchCmds.determineCmdType(); // must be the 2nd thing we do - if there is another line to be read from batch-file
            if ( this.verbose ) System.out.println( HDR +" START of while-loop for "+ _batchCmds.getState() +" .. for input=["+ toStringDebug(_input) +"]" );
            if ( _batchCmds.isLine2bEchoed() ) System.out.println( "Echo (As-Is): "+ _batchCmds.currentLine() );
            if ( _batchCmds.isLine2bEchoed() ) System.out.println( "Echo (Macro-substituted): "+  Macros.eval( this.verbose, _batchCmds.currentLine(), this.allProps ) );

            // start each loop, with an 'empty' placeholder Map, to collect output of current batch command
            tempOutput = getEmptyYAML();

            switch( _batchCmds.getCmdType() ) {
                case Cmd_MakeNewRoot:
                    final String newRootElem = Macros.eval( this.verbose, _batchCmds.getMakeNewRoot(), this.allProps );
                    tempOutput = getNewSingleYAMLEntry( newRootElem, "" ); // Very simple YAML:-    NewRoot: <blank>
                    this.runcount ++;
                    break;
                case Cmd_Batch:
                    final String bSubBatch = Macros.eval( this.verbose, _batchCmds.getSubBatchFile(), this.allProps );
                    tempOutput = this.go( bSubBatch, _input );
                    // technically, this.go() method is NOT meant to used recursively.  Semantically, this is NOT recursion :-(
                    this.runcount ++;
                    break;
                case Cmd_Properties:
                    tempOutput = this.onPropertyLineCmd( _batchCmds, _input, tempOutput );
                    this.runcount ++;
                    break;
                case Cmd_Foreach:
                    if ( this.verbose ) System.out.println( HDR +"\t'foreach'_cmd detected'");
                    if ( this.verbose ) System.out.println( HDR +"InputMap = "+ toStringDebug(_input) );
                    tempOutput = processFOREACHCmd_Step1( _batchCmds, _input  );
                    // since we processed the lines !!INSIDE!! the 'foreach' --> 'end' block .. via recursion.. we need to skip all those lines here.
                    skipInnerForeachLoops( _batchCmds, "processBatch(foreach)" );
                    this.runcount ++;
                    break;
                case Cmd_End:
                    if ( this.verbose ) System.out.println( HDR +"found matching 'end' keyword for 'foreach' !!!!!!! \n\n");
                    this.runcount ++;
                    return _input;
                    // !!!!!!!!!!!! ATTENTION : Function exits here SUCCESSFULLY / NORMALLY. !!!!!!!!!!!!!!!!
                    // break;
                case Cmd_SaveTo:
                    // Might sound crazy - at first.  inpMap for this 'saveAs' command is the output of prior command.
                    // final String saveTo_AsIs = _batchCmds.getSaveTo();
                    tempOutput = processSaveToLine( _batchCmds, _input );
                    // Input map is cloned before saving.. so the and Output Map is different (when returning from this function)
                    this.runcount ++;
                    break;
                case Cmd_UseAsInput:
                    tempOutput = processUseAsInputLine( _batchCmds );
                    this.runcount ++;
                    break;
                case Cmd_SetProperty:
                    final String key = Macros.eval( this.verbose, _batchCmds.getPropertyKV().key, this.allProps );
                    final String val = Macros.eval( this.verbose, _batchCmds.getPropertyKV().val, this.allProps );
                    globalVariables.setProperty( key, val );
                    if ( this.verbose ) System.out.println( HDR +" Cmd_SetProperty key=["+ key +"] & val=["+ val +"].");
                    break;
                case Cmd_Print:
                    tempOutput = this.onPrintCmd( _batchCmds, _input, tempOutput );
                    this.runcount ++;
                    break;
                case Cmd_YAMLLibrary:
                    if ( this.verbose ) System.out.println( HDR +" Setting YAMLLibrary ="+ _batchCmds.getYAMLLibrary() );
                    this.memoryAndContext.getContext().setYamlLibrary( _batchCmds.getYAMLLibrary() );
                    tempOutput = _input; // as nothing changes re: Input and Output Maps.
                    break;
                case Cmd_Verbose:
                    if ( this.verbose ) System.out.println( HDR +" this.verbose = =["+ this.verbose +"] & _batchCmds.getVerbose()=["+ _batchCmds.getVerbose() +"].");
                    this.verbose = _batchCmds.getVerbose();
                    tempOutput = _input; // as nothing changes re: Input and Output Maps.
                    break;
                case Cmd_Sleep:
                    System.err.println("\n\tsleeping for (seconds) "+ _batchCmds.getSleepDuration() );
                    Thread.sleep( _batchCmds.getSleepDuration()*1000 );
                    tempOutput = _input; // as nothing changes re: Input and Output Maps.
                    break;
                case Cmd_Any:
                    //This MUST ALWAYS be the 2nd last 'case' in this SWITCH statement
                    tempOutput = this.onAnyCmd( _batchCmds, _input );
                    this.runcount ++;
                    break;
                default:
                    System.out.println( HDR +"  unknown (new?) Batch-file command." );
                    System.exit(99);
            } // switch

            // this line below must be the very last line in the loop
            _input = tempOutput; // because we might be doing ANOTHER iteraton of the While() loop.
            this.verbose = _batchCmds.getVerbose(); // always keep checking the verbose level, which can change 'implicitly' within _batchCmds / BatchFileGrammerr.java

            if ( this.verbose ) System.out.println( HDR +" _________________________ BOTTOM of WHILE-loop: tempOutput =" + toStringDebug(tempOutput) +"");
        } // while loop

        // !!!!!!!!!!!!!!!!!!! ATTENTION: MOVE FOLLOWING LINE into SUBCLASS
        if ( this.verbose ) System.out.println( HDR +" ---@END---  tempOutput =" + toStringDebug(tempOutput) +"\n\n");
        // reached end of file.
        return tempOutput;
    }

    //=============================================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //=============================================================================

    /**
     *  Based on command type, process the inputNode and produce an output - for that specific command
     *  @param _batchCmds Non-Null instance of {@link BatchFileGrammer}
     *  @param _node Non-null instance of either the SnakeYaml library's org.yaml.snakeyaml.nodes.Node ( as generated by SnakeYAML library).. or.. EsotericSoftware Library's preference for LinkedHashMap&lt;String,Object&gt;, -- in either case, this object contains the entire Tree representing the YAML file.
     *  @return a BLANK/EMPTY/NON-NULL org.yaml.snakeyaml.nodes.Node object, as generated by SnakeYAML/CollectionsImpl library and you'll get the final YAML output representing all processing done by the batch file.  If there is any failure, either an Exception is thrown.
     *  @throws BatchCmdProcessor.BatchFileException if there is any issue with the command in the batchfile
     *  @throws Macros.MacroException if there is any issues with evaluating Macros.  This is extremely rare, and indicates a software bug.
     *  @throws java.io.FileNotFoundException specifically thrown by the SnakeYAML-library subclass of this
     *  @throws java.io.IOException Any issues reading or writing to PropertyFiles or to JSON/YAML files
     *  @throws Exception Any other unexpected error
     */
    protected abstract T  processFOREACHCmd_Step1( final BatchFileGrammer _batchCmds, T _node )
                throws BatchCmdProcessor.BatchFileException, Macros.MacroException, java.io.FileNotFoundException, java.io.IOException, Exception;

    //=============================================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //=============================================================================

    /**
     * When this function returns, the "pointer" within _batchCmds (.currentLine & .getLineNum()) ..
     *   should be pointing to the command AFTER the 'end' command.
     * This function basically keeps track of any inner foreachs .. and that's how it knows when the matching 'end' was detected.
     * @param _batchCmds pass-by-reference, so we can alter it's state and move it to the line AFTER matching 'end' commamd
     * @param _sInvoker for use in debugging output only (as there is tons of recursion-loops within these classes)
     * @throws BatchFileException
     * @throws Exception
     */
    protected void skipInnerForeachLoops( final BatchFileGrammer _batchCmds, final String _sInvoker )
                                    throws BatchFileException, Exception
    {
        final String HDR = CLASSNAME +": skipInnerForeachLoops("+_sInvoker+"): ";
        final int bookmark = _batchCmds.getLineNum();
        boolean bFoundMatchingENDCmd = false;
        int recursionLevel = 0;
        while ( _batchCmds.hasNextLine() ) {
            /* final String line22 = */ _batchCmds.nextLineOrNull(); // we do Not care what the line is about.
            _batchCmds.determineCmdType(); // must be the 2nd thing we do - if there is another line to be read from batch-file
            if ( this.verbose ) System.out.println( HDR +" skipping cmd "+ _batchCmds.getState() );

            final boolean bForEach22 = _batchCmds.isForEachLine();
            if ( bForEach22 ) recursionLevel ++;

            final boolean bEnd22 = _batchCmds.isEndLine();
            if ( bEnd22 ) {
                recursionLevel --;
                if ( recursionLevel < 0 ) {
                    bFoundMatchingENDCmd = true;
                    break; // we're done completely SKIPPING all the lines between 'foreach' --> 'end'
                } else
                    continue; // while _batchCmds.hasNextLine()
            } // if bEnd22
        }
        if (  !  bFoundMatchingENDCmd ) // sanity check.  These exceptions will get thrown if logic in 100 lines above isn't water-tight
            throw new BatchFileException( HDR +" ERROR In "+ _batchCmds.getState() +"] !!STARTING!! from line# "+ bookmark +".. do NOT see a MATCHING 'end' keyword following the  'foreach'.");
    }

    //======================================================================
    protected T processSaveToLine( final BatchFileGrammer _batchCmds, final T _node )
                                    throws Macros.MacroException,  java.io.IOException, Exception
    {
        final String HDR = CLASSNAME +": processSaveToLine(): ";
        final String saveTo_AsIs = _batchCmds.getSaveTo();
        if ( saveTo_AsIs != null ) {
            final String saveTo = Macros.eval( this.verbose, saveTo_AsIs, this.allProps );
            if ( this.memoryAndContext == null || this.memoryAndContext.getContext() == null )
                throw new BatchFileException( HDR +" ERROR In "+ _batchCmds.getState() +".. This program currently has NO/Zero memory from one line of the batch file to the next.  And a SaveTo line was encountered for ["+ saveTo +"]" );
            else {
                final T newnode = deepClone( _node );
                this.memoryAndContext.getContext().saveDataIntoReference( saveTo, newnode );
                return newnode;
            }
        } else 
            throw new BatchFileException( HDR +" ERROR In "+ _batchCmds.getState() +".. Missing or empty label for SaveTo line was encountered = ["+ saveTo_AsIs +"]" );
    }

    //=============================================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //=============================================================================

    // protected abstract T processUseAsInputLine( final BatchFileGrammer _batchCmds )
    //                             throws java.io.FileNotFoundException, java.io.IOException, Exception,
    //                             Macros.MacroException, BatchFileException;

    private T processUseAsInputLine( final BatchFileGrammer _batchCmds )
                                throws java.io.FileNotFoundException, java.io.IOException, Exception,
                                Macros.MacroException, BatchFileException
    {
        final String HDR = CLASSNAME +": processUseAsInputLine(): ";
        final String inputFrom_AsIs = _batchCmds.getUseAsInput();
        String inputFrom = Macros.eval( this.verbose, inputFrom_AsIs, this.allProps );
        inputFrom = new org.ASUX.common.StringUtils(this.verbose).removeBeginEndQuotes( inputFrom );

        if ( this.memoryAndContext == null || this.memoryAndContext.getContext() == null ) {
            throw new BatchFileException( HDR +"ERROR In "+ _batchCmds.getState() +".. This program currently has NO/Zero memory to carry it from one line of the batch file to the next.  And a useAsInput line was encountered for ["+ inputFrom +"]" );
        } else {
            final Object o = this.memoryAndContext.getContext().getDataFromReference( inputFrom );
            if ( instanceof_YAMLImplClass( o ) ) // o instanceof T <-- compiler cannot allow me to do this
            {   @SuppressWarnings("unchecked")
                final T retMap3 = (T) o;
                return retMap3;
            } else {
                final String es = (o==null) ? "Nothing in memory under that label." : ("We have type="+ o.getClass().getName()  +" = ["+ o.toString() +"]");
                throw new BatchFileException( HDR +"ERROR In "+ _batchCmds.getState() +".. Failed to read YAML/JSON from ["+ inputFrom_AsIs +"].  "+ es );
            }
        }
    }

    //=============================================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //=============================================================================

    protected T onPropertyLineCmd( final BatchFileGrammer _batchCmds, final T inputNode, final T tempOutput )
                            throws Macros.MacroException, java.io.FileNotFoundException, java.io.IOException
    {
        final Tuple<String,String> kv = _batchCmds.getPropertyKV(); // could be null, implying NOT a kvpair
        if ( kv != null) {
            final String kwom = Macros.eval( this.verbose, kv.key, this.allProps );
            final String fnwom = Macros.eval( this.verbose, kv.val, this.allProps );
            final Properties props = new Properties();
            props.load( new java.io.FileInputStream( fnwom ) );
            this.allProps.put( kwom, props ); // This line is the action taken by this 'PropertyFile' line of the batchfile
        }
        return inputNode; // as nothing changes re: Input and Output Maps.
    }


    protected T onPrintCmd( final BatchFileGrammer _batchCmds, final T inputNode, final T tempOutput )
                        throws Macros.MacroException, Exception
    {
        final String printExpression = _batchCmds.getPrintExpr();
        if ( this.verbose ) System.out.print( ">>>>>>>>>>>>> print line is ["+printExpression +"]" );
        if ( (printExpression != null) && (  !  printExpression.equals("-")) )  {
            String str2output = Macros.eval( this.verbose, printExpression, this.allProps );
            if ( str2output.trim().endsWith("\\n") ) {
                str2output = str2output.substring(0, str2output.length()-2); // chop out the 2-characters '\n'
                if ( str2output.trim().length() > 0 ) {
                    // the print command has text other than the \n character
                    final Object o = this.memoryAndContext.getDataFromMemory( str2output.trim() );
                    if ( o != null ) {
                        if ( instanceof_YAMLImplClass( o ) ) // o instanceof T <-- compiler cannot allow me to do this
                        {   @SuppressWarnings("unchecked")
                            final T oT = (T)o;
                            System.out.println( toStringDebug( oT ) );
                        } else
                            System.out.println( o ); // println (end-of-line character outputted)
                    } else
                        System.out.println( str2output ); // println (end-of-line character outputted)
                } else { // if length() <= 0 .. which prints all we have is a simple 'print \n'
                    System.out.println(); // OK. just print a new line, as the print command is a simple 'print \n'
                }
            } else {
                final Object o = this.memoryAndContext.getDataFromMemory( str2output.trim() );
                if ( o != null ) {
                    if ( instanceof_YAMLImplClass( o ) ) // o instanceof T <-- compiler cannot allow me to do this
                    {   @SuppressWarnings("unchecked")
                        final T oT = (T)o;
                        System.out.print( toStringDebug( oT ) ); // Note: print only.  NO EOL character outputted.
                    } else
                        System.out.print( o ); // Note: print only.  NO EOL character outputted.
                } else
                    System.out.print( str2output +" " ); // print only.  NO EOL character outputted.
            }
            // ATTENTION!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            // DO NOT COMMENT THIS ABOVE.  Do NOT ADD AN IF CONDITION to this.  This is by design.
            System.out.flush();
        } else {
            // if the command/line is just the word 'print' .. print the inputNode
            System.out.println( toStringDebug(inputNode) );
        }
        return inputNode; // as nothing changes re: Input and Output Maps.
    }

    //==============================================================================
    private T onAnyCmd( final BatchFileGrammer _batchCmds, final T _input )
                    throws BatchFileException, Macros.MacroException, java.io.FileNotFoundException, java.io.IOException, Exception
    {
        final String HDR = CLASSNAME + ": onAnyCmd(): ";
        final String cmd_AsIs = _batchCmds.getCommand();
        assert ( cmd_AsIs == null );
        final String cmdStrNM = Macros.eval( this.verbose, cmd_AsIs, this.allProps ).trim();
        assert ( cmdStrNM != null );

        final String fullCmdLine = _batchCmds.currentLine() + " -i - -o -"; // Adding the '-i' and '-o' is harmless, but required because CmdLineArgs.java will barf otherwise (as CmdLineArgs.java thinks it's being run on commandline by a user)

        final boolean isYAMLCmd = cmdStrNM.equals("yaml");
        final boolean isAWSCmd = cmdStrNM.equals("aws.sdk");

        //--------------------------------
        // We need to invoke constructor of the SUB-CLASS of org.ASUX.yaml.CmdInvoker - from the appropriate YAML-Library or AWS-SDK Library.
        // For that let's gather the Constructor parameter-types and arguments
        String implClassNameStr = null;
        String cmdArgsClassNameStr = null;
        Class[] paramClassList;
        Object[] methodArgs;

        if ( isYAMLCmd ) {
            implClassNameStr = "org.ASUX.YAML.NodeImpl.CmdInvoker";
            cmdArgsClassNameStr = "org.ASUX.YAML.NodeImpl.CmdLineArgs";
            paramClassList  = new Class[] { boolean.class, boolean.class, MemoryAndContext.class, this.memoryAndContext.getContext().getLibraryOptionsClass() };
            methodArgs      = new Object[] { this.verbose, this.showStats, this.memoryAndContext, this.memoryAndContext.getContext().getLibraryOptionsObject() };

        } else if ( isAWSCmd ) {
            implClassNameStr = "org.ASUX.AWSSDK.CmdInvoker";
            cmdArgsClassNameStr = "org.ASUX.AWSSDK.CmdLineArgsAWS";
            paramClassList  = new Class[] { boolean.class, boolean.class,           this.memoryAndContext.getContext().getLibraryOptionsClass() };
            methodArgs      = new Object[] { this.verbose, this.showStats,          this.memoryAndContext.getContext().getLibraryOptionsObject() };

        } else {
            throw new BatchFileException( "Unknown Batchfile command ["+ cmd_AsIs +"] / ["+ cmdStrNM +"] in "+ _batchCmds.getState() );
        }

        assert( implClassNameStr != null );
        if ( this.verbose )  System.out.println( HDR +"implClassNameStr ="+ implClassNameStr );

        //--------------------------------
        // fullCmdLine was enhanced in the above IF-ELSE
        String[] cmdLineArgsStrArr = convStr2Array( fullCmdLine );
        cmdLineArgsStrArr = java.util.Arrays.copyOfRange( cmdLineArgsStrArr, 1, cmdLineArgsStrArr.length ); // get rid of the 'yaml' or 'aws.sdk' word at the beginning of the command
        final Class[] mainArgsClassList = new Class[] { cmdLineArgsStrArr.getClass() };
        final Object[] mainArgs         =  new Object[] { cmdLineArgsStrArr };

        //--------------------------------
        // Now invoke constructor of the SUB-CLASS of org.ASUX.yaml.CmdInvoker - from the appropriate YAML-Library or AWS-SDK Library.
        org.ASUX.yaml.CmdInvoker newCmdinvoker;
        org.ASUX.yaml.CmdLineArgsCommon newCmdLineArgsObj;

        // Do the equivalent of:- new org.ASUX.YAML.NodeImpl.CmdInvoker( this.verbose, this.showStats, .. .. );
        // Do the equivalent of:- new org.ASUX.AWSSDK.CmdInvoker( this.verbose, this.showStats, .. .. );
        try {
            if ( this.verbose ) System.out.println( HDR +"about to invoke "+ implClassNameStr +".constructor() and "+ cmdArgsClassNameStr +".create()." );

            final Class<?> implClass = Cmd.class.getClassLoader().loadClass( implClassNameStr ); // returns: protected Class<?> -- throws ClassNotFoundException
            if ( this.verbose )  System.out.println( HDR +"implClassNameStr=["+ implClassNameStr +"] successfully loaded using ClassLoader.");
            final Object oo = org.ASUX.common.GenericProgramming.invokeConstructor( implClass, paramClassList, methodArgs );
            if ( this.verbose ) System.out.println( HDR +"returned from successfully invoking "+ implClassNameStr +".constructor()." );

            newCmdinvoker = (org.ASUX.yaml.CmdInvoker) oo;

            final Class<?> cmdArgsClass = Cmd.class.getClassLoader().loadClass( cmdArgsClassNameStr ); // returns: protected Class<?> -- throws ClassNotFoundException
            if ( this.verbose )  System.out.println( HDR +"cmdArgsClassNameStr=["+ cmdArgsClassNameStr +"] successfully loaded using ClassLoader.");
            final Object oo2 = org.ASUX.common.GenericProgramming.invokeStaticMethod( cmdArgsClass, "create", mainArgsClassList, mainArgs );
            if ( this.verbose ) System.out.println( HDR +"returned from successfully invoking "+ cmdArgsClassNameStr +".create()." );

            newCmdLineArgsObj = (org.ASUX.yaml.CmdLineArgsCommon) oo2;
            newCmdLineArgsObj.verbose = this.verbose; // pass on whatever this user specified on cmdline re: --verbose or not.
            if ( this.verbose ) System.out.println( HDR +"newCmdLineArgsObj="+ newCmdLineArgsObj.toString() );

        } catch (ClassNotFoundException e) {
            final String estr = "ERROR In "+ _batchCmds.getState() +".. Failed to run the command in current line.";
            if ( this.verbose ) e.printStackTrace(System.err);
            if ( this.verbose ) System.err.println( HDR + estr +"\n"+ e );
            throw new BatchFileException( e.getMessage() );
        } catch (Exception e) {
            final String estr = "ERROR In "+ _batchCmds.getState() +".. Failed to run the command in current line.";
            if ( this.verbose ) e.printStackTrace(System.err);
            if ( this.verbose ) System.err.println( HDR + estr +"\n"+ e );
            throw new BatchFileException( e.getMessage() );
        }

        //--------------------------------
        if ( newCmdLineArgsObj.quoteType == Enums.ScalarStyle.UNDEFINED )
            newCmdLineArgsObj.quoteType = this.quoteType;

        if ( isYAMLCmd ) {
            newCmdinvoker.setYamlLibrary( this.memoryAndContext.getContext().getYamlLibrary() );
            if (this.verbose) System.out.println( HDR +" set YAML-Library to [" + this.memoryAndContext.getContext().getYamlLibrary() + " and [" + newCmdinvoker.getYamlLibrary() + "]" );

        } else if ( isAWSCmd ) {

        } else {
            // We must have a previous replica of this same IF-ELSE-ELSE above.  So, how come we're still here in this block?
            System.err.println( HDR +"FATAL ERROR: Unknown Batchfile command ["+ cmd_AsIs +"] / ["+ cmdStrNM +"] in "+ _batchCmds.getState() );
            System.exit(61);
        }

        //--------------------------------
        // We expect the underlying library to generate the object of type T for the return value of newCmdinvoker.processCommand().
        @SuppressWarnings("unchecked")
        final T output = (T) newCmdinvoker.processCommand( newCmdLineArgsObj, _input );
        if (this.verbose) System.out.println( HDR +" processing of command returned [" + (output==null?"null":output.getClass().getName()) + "]" );
        return output;

    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

    private String[] convStr2Array( final String _cmdStr )
                            throws Macros.MacroException, java.io.IOException
    {
        final String HDR = CLASSNAME + ": convStr2Array(): ";
        String [] cmdLineArgsStrArr = null;
        if (this.verbose) System.out.println( HDR +"_cmdStr="+ _cmdStr );
        String cmdStrCompacted = _cmdStr.replaceAll("\\s\\s*", " "); // replace multiple spaces with a single space.
        // cmdStrCompacted = cmdStrCompacted.trim(); // no need.  The _batchCmds already took care of it.
        final String cmdStrNoMacros = Macros.eval( this.verbose, cmdStrCompacted, this.allProps ).trim();
        if (this.verbose) System.out.println( HDR +"cmdStrCompacted = "+ cmdStrCompacted );

        // https://mvnrepository.com/artifact/com.opencsv/opencsv
        final java.io.StringReader reader = new java.io.StringReader( cmdStrNoMacros );
        final com.opencsv.CSVParser parser = new com.opencsv.CSVParserBuilder().withSeparator(' ').withQuoteChar('\'').withIgnoreQuotations(false).build();
        final com.opencsv.CSVReader cmdLineParser = new com.opencsv.CSVReaderBuilder( reader ).withSkipLines(0).withCSVParser( parser ).build();
        cmdLineArgsStrArr = cmdLineParser.readNext(); // pretend we're reading the 1st line ONLY of a CSV file.
        if (this.verbose) { System.out.print( HDR +"cmdLineArgsStrArr = ");  for( String s: cmdLineArgsStrArr) System.out.println(s+"\t"); System.out.println(); }
        // some of the strings in this.cmdLineArgsStrArr may still have a starting and ending single/double-quote
        cmdLineArgsStrArr = new org.ASUX.common.StringUtils(this.verbose).removeBeginEndQuotes( cmdLineArgsStrArr );
        if (this.verbose) { System.out.print( HDR +"cmdLineArgsStrArr(REMOVEDALLQUOTES) = ");  for( String s: cmdLineArgsStrArr) System.out.print(s+"\t"); System.out.println(); }
        return cmdLineArgsStrArr;
    }

    //=======================================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    //=======================================================================

    // For unit-testing purposes only
    public static void main(String[] args) {
        // try {
        //     final BatchCmdProcessor o = new BatchCmdProcessor(true, true);
        //     T inpMap = null;
        //     T outpMap = o.go( args[0], inpMap );
        // } catch (Exception e) {
        //     e.printStackTrace(System.err); // main() method for unit-testing
        // }
    }

}