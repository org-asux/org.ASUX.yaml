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

import java.util.ArrayList;

import org.apache.commons.cli.*;

/** <p>This class is a typical use of the org.apache.commons.cli package.</p>
 *  <p>This class has No other function - other than to parse the commandline arguments and handle user's input errors.</p>
 *  <p>For making it easy to have simple code generate debugging-output, added a toString() method to this class.</p>
 *  <p>Typical use of this class is: </p>
 *<pre>
 public static void main(String[] args) {
 cmdLineArgs = new CmdLineArgsBasic(args);
 .. ..
 *</pre>
 *
 *  <p>See full details of how to use this, in {@link org.ASUX.yaml.Cmd} as well as the <a href="https://github.com/org-asux/org.ASUX.cmdline">org.ASUX.cmdline</a> GitHub.com project.</p>
 * @see org.ASUX.yaml.Cmd
 */
public class CmdLineArgsBasic {

    public static final String CLASSNAME = CmdLineArgsBasic.class.getName();

    public static final String[] READCMD = { "r", "read", "output all YAML-elements that match" };
    public static final String[] LISTCMD = { "l", "list", "List YAML-Keys (lhs) that match" };
    public static final String[] INSERTCMD = { "n", "insert", "insert new element (json-string parameter) @ the locations identified by the YAML path" };
    public static final String[] REPLACECMD = { "c", "replace", "change/replace all elements that match with json-string provided on cmdline" };
    public static final String[] DELETECMD = { "d", "delete", "Delete all elements that match" };
    public static final String[] TABLECMD = { "t", "table", "produce a tabular output like a traditional SQL-query would" };
    // public static final char REPLACECMDCHAR = 'c'; // -c === --replace
    public static final String[] MACROCMD = { "m", "macro", "run input YAML file thru a MACRO processor searching for ${ASUX::__} and replacing __ with values from Properties file" };
    public static final String[] BATCHCMD = { "b", "batch", "run a batch of commands, which are listed in the <batchfile>" };

    public static final String YAMLLIB = "yamllibrary";

    //------------------------------------
    final ArrayList<String> args = new ArrayList<>();
    public boolean verbose = false;

    public YAML_Libraries YAMLLibrary = YAML_Libraries.ESOTERICSOFTWARE_Library;

    //------------------------------------
    private final CmdLineArgs cmdLineArgs;

    public CmdEnum cmdType = CmdEnum.UNKNOWN;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /** Constructor.
     *  @param args command line argument array - as received as-is from main().
     *  @throws Exception like ClassNotFoundException while trying to serialize and deserialize the input-parameter
     */
    public CmdLineArgsBasic(String[] args) throws Exception
    {
        this.args.addAll( java.util.Arrays.asList(args) );

        //----------------------------------
        Options options = new Options();
        Option opt;

        opt= new Option("v", "verbose", false, "Show debug output");
        opt.setRequired(false);
        options.addOption(opt);

        //----------------------------------
        opt = new Option("zy", YAMLLIB, false, "only valid values are: "+ YAML_Libraries.list("\t") );
        opt.setRequired(false);
        opt.setArgs(1);
        opt.setOptionalArg(false);
        opt.setArgName("yamllibparam");
        options.addOption(opt);

        //----------------------------------
        OptionGroup grp = new OptionGroup();
        Option readCmdOpt = new Option( READCMD[0], READCMD[1], true, READCMD[2] );
            readCmdOpt.setOptionalArg(false);
            // readCmdOpt.setArgs(1);
            // readCmdOpt.setArgName("YAMLPattern");
        Option listCmdOpt = new Option( LISTCMD[0], LISTCMD[1], true, LISTCMD[2] );
            listCmdOpt.setOptionalArg(false);
            // listCmdOpt.setArgs(1);
            // listCmdOpt.setArgName("YAMLPattern");
        Option insCmdOpt = new Option( INSERTCMD[0], INSERTCMD[1], true, INSERTCMD[2] );
            insCmdOpt.setOptionalArg(false);
            // insCmdOpt.setArgs(2);
            // insCmdOpt.setValueSeparator(' ');
            // insCmdOpt.setArgName("YAMLPattern> <newValue"); // Note: there's a trick in the parameter-string.. as setArgName() assumes a single 'word' and puts a '<' & '>' around that single-word.
        Option replCmdOpt = new Option( REPLACECMD[0], REPLACECMD[1], true, REPLACECMD[2] );
            replCmdOpt.setOptionalArg(false);
            // replCmdOpt.setArgs(2);
            // replCmdOpt.setValueSeparator(' ');
            // replCmdOpt.setArgName("YAMLPattern> <newValue"); // Note: there's a trick in the parameter-string.. as setArgName() assumes a single 'word' and puts a '<' & '>' around that single-word.
        Option delCmdOpt = new Option( DELETECMD[0], DELETECMD[1], true, DELETECMD[2] );
            delCmdOpt.setOptionalArg(false);
            // delCmdOpt.setArgs(1);
            // delCmdOpt.setArgName("YAMLPattern");
        Option tableCmdOpt = new Option( TABLECMD[0], TABLECMD[1], false, TABLECMD[2] );
            tableCmdOpt.setOptionalArg(false);
            // tableCmdOpt.setArgs(2);
            // tableCmdOpt.setArgName("YAMLPattern> <column,column");
        Option macroCmdOpt = new Option( MACROCMD[0], MACROCMD[1], true, MACROCMD[2] );
            macroCmdOpt.setOptionalArg(false);
            // macroCmdOpt.setArgs(1);
            // macroCmdOpt.setArgName("propertiesFile");
        Option batchCmdOpt = new Option( BATCHCMD[0], BATCHCMD[1], true, BATCHCMD[2] );
            batchCmdOpt.setOptionalArg(false);
            // batchCmdOpt.setArgs(1);
            // batchCmdOpt.setArgName("batchFile");
        grp.addOption(readCmdOpt);
        grp.addOption(listCmdOpt);
        grp.addOption(tableCmdOpt);
        grp.addOption(delCmdOpt);
        grp.addOption(insCmdOpt);
        grp.addOption(replCmdOpt);
        grp.addOption(macroCmdOpt);
        grp.addOption(batchCmdOpt);
        grp.setRequired(true);

        options.addOptionGroup(grp);

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        // temp variable, to help set a 'final' class variable
        org.ASUX.yaml.CmdLineArgs cla = null;

        org.apache.commons.cli.CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.HelpFormatter formatter = new HelpFormatter();
        org.apache.commons.cli.CommandLine cmd;

        try {

            // if ( ???.verbose ) ..
            // what if the parse() statement below has issues.. ?  We can't expect to use this.apacheCmd.hasOption("verbose") 
// System.err.print( CLASSNAME +" parse(): _args = "+ args +"  >>>>>>>>>>>>> "); for( String s: args) System.out.print(s+"\t");  System.out.println();
// System.err.println( CLASSNAME +" parse(): this = "+ this.toString() );
            cmd = parser.parse( options, args, true ); //3rd param: boolean stopAtNonOption

            this.verbose = cmd.hasOption("verbose");

            if ( cmd.getOptionValue(YAMLLIB) != null )
                this.YAMLLibrary = YAML_Libraries.fromString( cmd.getOptionValue(YAMLLIB) );
            else
                this.YAMLLibrary = YAML_Libraries.SNAKEYAML_Library; // default.

            //----------------------------------------------
            if ( cmd.hasOption(READCMD[1]) ) {
                this.cmdType = CmdEnum.READ;
                // this.yamlRegExpStr = cmd.getOptionValue(READCMD);
                cla = new CmdLineArgs( this.cmdType, READCMD[0], READCMD[1], READCMD[2], 1, "YAMLPattern" );
            }
            if ( cmd.hasOption(LISTCMD[1]) ) {
                this.cmdType = CmdEnum.LIST;
                cla = new CmdLineArgs( this.cmdType, LISTCMD[0], LISTCMD[1], LISTCMD[2], 1, "YAMLPattern" );
            }
            if ( cmd.hasOption(INSERTCMD[1]) ) {
                this.cmdType = CmdEnum.INSERT;
                final CmdLineArgsInsertCmd insertCmdLineArgs = new CmdLineArgsInsertCmd( this.cmdType, INSERTCMD[0], INSERTCMD[1], INSERTCMD[2], 2, "YAMLPattern> <newValue" );
                cla = insertCmdLineArgs;
            }
            if ( cmd.hasOption(DELETECMD[1]) ) {
                this.cmdType = CmdEnum.DELETE;
                cla = new CmdLineArgs( this.cmdType, DELETECMD[0], DELETECMD[1], DELETECMD[2], 1, "YAMLPattern" );
            }
            if ( cmd.hasOption(REPLACECMD[1]) ) {
                this.cmdType = CmdEnum.REPLACE;
                final CmdLineArgsReplaceCmd replaceCmdLineArgs = new CmdLineArgsReplaceCmd( this.cmdType, REPLACECMD[0], REPLACECMD[1], REPLACECMD[2], 2, "YAMLPattern> <newValue" );
                cla = replaceCmdLineArgs;
            }
            if ( cmd.hasOption(TABLECMD[1]) ) {
                this.cmdType = CmdEnum.TABLE;
                final CmdLineArgsTableCmd tableCmdLineArgs = new CmdLineArgsTableCmd( this.cmdType, TABLECMD[0], TABLECMD[1], TABLECMD[2], 2, "YAMLPattern> <column,column" );
                cla = tableCmdLineArgs;
            }
            if ( cmd.hasOption(MACROCMD[1]) ) {
                this.cmdType = CmdEnum.MACRO;
                final CmdLineArgsMacroCmd macroCmdLineArgs = new CmdLineArgsMacroCmd( this.cmdType, MACROCMD[0], MACROCMD[1], MACROCMD[2], 1, "propertiesFile" );
                cla = macroCmdLineArgs;
            }
            if ( cmd.hasOption(BATCHCMD[1]) ) {
                this.cmdType = CmdEnum.BATCH;
                final CmdLineArgsBatchCmd batchCmdLineArgs = new CmdLineArgsBatchCmd( this.cmdType, BATCHCMD[0], BATCHCMD[1], BATCHCMD[2], 1, "batchFile" );
                cla = batchCmdLineArgs;
            }

            //----------------------------------------------
            // let the subclassses do the parsing based on individual command's needs
            this.cmdLineArgs = cla;
            this.cmdLineArgs.parse( args );

        } catch (ParseException e) {
            e.printStackTrace(System.err);
            formatter.printHelp("java <jarL> "+ CLASSNAME, options);
            throw e;
        }
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /** For making it easy to have simple code generate debugging-output, added this toString() method to this class.
     */
    public String toString() {
        // this.args.forEach(s -> System.out.println(s+"\t") );
        return this.args.toString();
        // return "verbose="+verbose;
    }

    //------------------------------------
    /**
     * This object reference is either to a CmdLineArgs class (for READ, LIST and DELETE commands), or subclasses (for INSERT, REPLACE, TABLE, MACRO, BATCH commands)
     */
    public CmdLineArgs getSpecificCmd() {
        return this.cmdLineArgs;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    // For unit-testing purposes only
//    public static void main(String[] args) {
//        new CmdLineArgsBasic(args);
//    }

}