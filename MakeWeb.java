import java.io.File;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.List;
import java.util.Properties;
import java.util.Date;

import java.text.DateFormat;



// for JSON processing

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

/*
 * read some input and make a typical Swanson photo album
 * web page
 *
 * Updated:
 *
 * Thu 09 Jul 2020 10:08:42 AM CDT
 *
 * BUG!
 *
 * a) alternate "alt=" attributes do not have ending single quotes! this can really
 *    mess up the browser
 *
 * b) since the "alt=" uses single quotes, embedded single quotes can wreak havoc
 *    with web rendering. Change them to &quot; entities
 *
 *
 * 1) change to get project name from command line, then
 *    use that for filenames (NOTE project name is in JSON, too, but
 *    that is not used)
 * 2) finish work on reading input and making photo rows
 *
 */
public class MakeWeb
{


    public Properties key_values = null; // will be filled from JSON
    
    public List project_specific_metadata = null; // filled from outside JSON
	public List front1; // holds part 1 of front matter
	public List back1; // holds final HTML matter
	public PrintWriter pW = null;
	public BufferedReader iN = null;
	public Map project_keys = null; // will be populated from JSON
	public String previous_line = "NO PREV";
	/*
	 * following project key values are treated specially
	 */
    String[] special_keys = {
    	"PROJECT_TEXT", // 0 == text will be split with colons into individual paragraphs
    	"PROJECT_DESCRIPTION", // 1 == use and add more text
    	"PROJECT_DATE" // 2 == generated by this program
    };

    public final static void main(String[] args)
    {
        try
        {
		MakeWeb this_is_it = new MakeWeb();  
		this_is_it.init(args); // args[0] is project name
		this_is_it.execute();
		this_is_it.finish();
	}
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    } // end main

	public void init(String[] args) throws Exception
	{
		/*
		 * project name is on command line
		 * 
		 */ 
		if (args.length < 1)
		{
			throw new Exception("Must specify project name, argument 1");
		}
		String projname = args[0];

		/* 
		 * project info are key/value pairs
		 * 
		 */
	    List proj =  getProjectSpecificMetaData(projname + "_project.json");
//		System.out.println("proj: " + proj);
	    populate_key_values(proj);

		/*
		 * open filenames, based on project name (from command line)
		 */

		pW = new PrintWriter(new FileWriter(projname + "_test.html"));
		iN = new BufferedReader(new FileReader(projname + "_input.txt"));
		/*
		 * read various boilerplate items for web output
		 */
	    Map stmap = readJSON( "boilerplate", true);
		//System.out.println("boiler: " + stmap);
		front1 = (List)stmap.get("front1");
		back1 = (List)stmap.get("back1");
		/*
		 * now that the Lists of content have been fetched
		 * use the PROJECT-SPECIFIC stuff to insert desired
		 * fields IN PLACE
		 */
		modifyMetaData();
//		System.out.println("front1: " + front1);
//		System.out.println("back1: " + back1);
	} // init


	public void execute() throws Exception
	{
		/*
		 * boilerplate has now been modified IN PLACE
		 * Values were inserted using the 'properties'
		 * JSON initially read
		 */

		// write modified front matter
		createAPage(pW,front1,false);

		writePhotoMaterial(iN,pW);

		// write modified end matter
		createAPage(pW,back1,false);

	}

	/*
	 * Caption can be much of any writing, but that writing can get
	 * REALLY mess up the HTML. So, we'll try to remove as much bad stuff as
	 * possible. This includes single quotes (apostrophe), which are used within
	 * the HTML tags. Use of HTML tags within the caption is kinda OK, so
	 * we WON'T escape &gt; and &lt; characters
	 */
	public String escapeCaption(String cap)
	{
		String newer = cap.replaceAll("&","&amp;");   // escape and signs with XML-compliant stuff
		String newer2 = newer.replaceAll("'","&apos;");   // escape single quote (apostrophe) with entity
		return newer2;
	}

	public void writePhotoMaterial(
		BufferedReader inn,
		PrintWriter pr) throws Exception
	{

		int read_state = 0; // looking for first 4-line group
		boolean left_side = true; // switch back and forth

		while (true)
		{

			/*
			 * ALL photo data in 4-line groups, plain text
			 */
			String caption1 = inn.readLine();
			if (caption1 == null)
			{
				if (read_state == 0) 
				{
					throw new Exception("Parse error, missing line 1 of 4-line group. Previous line: " + previous_line);
				}
				else
				{
					break; // eof in right place, quit photos
				}
			} // check for eof
			read_state = 1; // now reading subsequent 4-line groups
			previous_line = caption1;

			String caption2 = inn.readLine();
			if (caption2 == null)
			{
				throw new Exception("Parse error, missing line 2 of 4-line group. Previous line: " + previous_line);
			}
			previous_line = caption2;

			String thumb_url = inn.readLine();
			if (thumb_url == null)
			{
				throw new Exception("Parse error, missing line 3 of 4-line group. Previous line: " + previous_line);
			}
			previous_line = thumb_url;

			String pic_url = inn.readLine();
			if (pic_url == null)
			{
				throw new Exception("Parse error, missing line 4 of 4-line group. Previous line: " + previous_line);
			}
			previous_line = pic_url;

			/*
			 * have read 4 line and now have the fields, write rows of them in left/right arrangement
			 */
			// write header regardless of left vs right
			pr.println("<section class='p-strip--accent'>"); // each in an 'accent' section
			pr.println("<div class='row'>"); // new row
			pr.println("<div class='col-6 u-vertically-center'>");  //  both sides same size
			if (left_side)
			{
				// left, row already started
				pr.println(" <p>  <span class='pic_title'>" + escapeCaption(caption1));  //  text starts with title
				pr.println("</span> -- " +
				  caption2 + "</p> </div> <!-- end text group -->\n" + "<div class='col-6 u-vertically-center'> <p> <br/>");
				pr.println("<a href='" +
				  pic_url + "'>");  
				pr.println("<img src='" +
				  thumb_url + "' alt='[" +  // this is correct!
				  escapeCaption(caption1) + "]' style='color:black;border:solid;border-width:2px'/></a> </p>");   
				pr.println("</div> <!-- end picture group --> </div> <!-- end row -->");
				pr.println("</section>"); // each in an accent section
				left_side = false; // switch sides
			} // end if left side
			else
			{
				// right, row already started
				pr.println(" <p> <br/>");
				pr.println("<a href='" +
					pic_url + "'>");
				pr.println("<img src='" +
					thumb_url + "' alt='[" +  // following was WRONG, now fixed
					escapeCaption(caption1) + "]' style='color:black;border:solid;border-width:2px'/></a> </p>");
				pr.println("</div> <!-- end picture group -->");
				pr.println("<div class='col-6 u-vertically-center'> <p>  <span class='pic_title'>" +
					escapeCaption(caption1)); // text starts with title
				pr.println("</span> --  " +
					caption2 + "</p> </div> <!-- end text group -->");  
				pr.println("</div> <!-- end row -->");
				pr.println("</section>"); //  each in an accent section
				left_side = true; // back again
			} // end right side
		} // end while reading 4-line groups
/*
foreach (@thumbs)
{
	#print;
	#print "\n";
	$this_thumb = $_;
	push @out, '<section class="p-strip--accent">' . "\n"; ## each in an 'accent' section
	push @out,  '<div class="row">' . "\n"; # new row
	push @out ,  '<div class="col-6 u-vertically-center">'; # both sides same size
	if ($left_right == 0)
	{
		# left, row already started
		push @out,   ' <p>  <span class="pic_title">  ' . "$titles[$position]\n"; # text starts with title
		push @out,  "</span> -- $texts[$position]\n</p> </div> <!-- end text group -->" . '<div class="col-6 u-vertically-center"> <p> <br/>';
		push @out, '<a href="' . $pics[$position] . '">';
		push @out,   '<img src="' . $this_thumb . '" alt="[' .
			$titles[$position] . ']" style="color:black;border:solid;border-width:2px"/></a> </p>' . "\n";
		push @out,   "</div> <!-- end picture group --> </div> <!-- end row -->\n";
		push @out,  "</section>\n"; ## each in an accent section
		$left_right = 1;
	} ## end left
	else
	{
		# right, row already started
		push @out,  ' <p> <br/>';
		push @out,  '<a href="' . $pics[$position] . '">';
		push @out,  '<img src="' . $this_thumb . '" alt="[' .
			$titles[$position] . ']" style="color:black;border:solid;border-width:2px"/></a> </p>' . "\n";
		push @out,  "</div> <!-- end picture group --> \n";
		push @out,  '<div class="col-6 u-vertically-center"> <p>  <span class="pic_title">  ' . "$titles[$position]\n"; # text starts with title
		push @out,   "</span> --  $texts[$position]\n</p> </div> <!-- end text group -->\n";
		push @out,  "</div> <!-- end row -->\n";
		push @out,  "</section>\n"; ## each in an accent section
		$left_right = 0; # alternate
	}
	$position++; ## advance to next items in lists
} ## end for each pic
*/
	} // end write photo material

	public void finish() throws Exception
	{
		iN.close(); // close input file
		pW.close(); // terminate HTML output
		pW.flush(); // terminate HTML output
	}




	// populate key values from List
    public void populate_key_values(List keyvalues) throws Exception
    {
        key_values = new Properties(); // empty to start
        if (keyvalues == null)
        {
            System.out.println("Boilerplate strings do not exist.");
            return;  // return with empty treemap
        }
        if (keyvalues.size() == 0)
        {
            System.out.println("Boilerplate strings empty.");
            return;  // return with empty treemap
        }
        /* 
         * the array must be even, each odd item is key, even
         * value is the value. These are stored in the Map
         */
        Object someobject = null;
        String key = "";
        List arr = keyvalues;
        // debug System.out.println("Boilerplate strings:" + arr.size());
        if (arr.size() %2 != 0)
        {
            throw new Exception("Problems with JSON, boilerplate list is not even sized: " + 
                                arr.size());
        }
        for (int ii = 0 ; ii < arr.size() ; ii += 2)
        {
            someobject = arr.get(ii);
            if (!(someobject instanceof String))
            {
                throw new Exception("Problem with boilerplate at: " + ii + 
                                    " not string: " + someobject);
            }
            key = (String)someobject; // will be key
            someobject = arr.get(ii + 1);
            if (!(someobject instanceof String))
            {
                throw new Exception("Problem with boilerplate at: " + (ii+1) + 
                                    " not string: " + someobject);
            }
            key_values.setProperty(key,(String)someobject); // populate the key value pairs
        } // end loop on the strings in boilerplate
        // System.out.println("KeyValues: " + key_values);  // debugging
    } // end populate the key-value pairs
	
    /*
     * return a value for a key. If not found, return
     * null, let the caller figure out what they
     * did wrong
     */
    public String gT(String key) throws Exception
    {
        if (key_values == null)
        {
            throw new Exception("Boilerplate is empty!");
        }
        if (key_values.containsKey(key))
        {
            return (String)key_values.get(key);
        }
        else
        {
            // not found
            throw new Exception("Boilerplate missing key: " + key);
        }
    } // gT, general string getter
    

    
    public List getProjectSpecificMetaData(String filename) throws Exception
    {
        File input = new File(filename);
        Map<String,Object> userData = 
		readJSON(input,false); // no debugging msg
        
        // userData is a Map containing the named arrays
        
            project_specific_metadata = (List)userData.get("project");
    		return project_specific_metadata;
    } // end get project specific metadata


	// replace in List object (if not a List, cast will fail)
	public void stringReplacer(Object a, Map project_keys) throws Exception
	{
	
//System.out.println("stringReplacer (obj) called in special content creator");
		String akey = "";
		ReplacementString rval = null;
		String aval = "";
		if (a instanceof List)
		{
			List arr = (List)a; // cast
			// now this object contains ONLY strings
			// we must be able to replace its contents
			// after alteration. Thus we use
			// ListIterator!
			//
			ListIterator ii = arr.listIterator();
			boolean did_something = false;

			while (ii.hasNext())
			{
				String test = (String)ii.next(); // has to be
				Iterator inner = project_keys.keySet().iterator(); // all search keys
				while (inner.hasNext())
					 {
					 	akey = (String)inner.next();
					 	if (test.indexOf(akey ) >= 0)
					 	{
					 		// HIT IT!
					 		did_something = true;
					 		rval = (ReplacementString)project_keys.get(akey);
							String result = replaceAString(test,akey,rval); // get replacement, whether normal or special							
					 		ii.set(result); // overwrites current boilerplate string
						
					 	} // end if found one of the keys
					 } // end check all keys (may be more than one in a line)
			} // end pass all strings inside the list
			if (did_something) // debugging
			{
				Iterator see = arr.iterator();
				while (see.hasNext())
				{
					// if debugging uncomment these 2 lines
					//System.out.println("R: " + 
					//	see.next());
					// if NOT debugging uncomment this
					see.next();
				}
			} // end if did something, possible debug output
		} // end if correct object
		else
		{
			throw new Exception("Problems with string_replacement, internal boilerplate  : " + a.getClass().getName());
			
		}
 
	} // end stringReplacer (Object)

	/*
	 * GENERIC string replacer. Assumes that
	 * the List passed is simply Strings, and nothing else
	 */
	public void stringReplacer(List a, Map project_keys) throws Exception
	{
/*
System.out.println("stringReplacer (list) called in special content creator");
System.out.println("  keys: " + project_keys);
*/
		String akey = "";
		ReplacementString rval = null;
		String aval = "";
		List arr = a;
			// now this object contains ONLY strings
			// we must be able to replace its contents
			// after alteration. Thus we use
			// ListIterator!
			//
			ListIterator ii = arr.listIterator();
			boolean did_something = false;

			while (ii.hasNext())
			{
				String test = (String)ii.next(); // has to be
//System.out.println("   testing: " + test);
				Iterator inner = project_keys.keySet().iterator(); // all search keys
				while (inner.hasNext())
					 {
					 	akey = (String)inner.next();
/*
if (test.indexOf("HTML_CSS") >= 0)
{
	System.out.println("Testing: " + test + ", against: " + akey);
}
*/
					 	if (test.indexOf(akey ) >= 0)
					 	{
//System.out.println("    matched: " + akey);
					 		// HIT IT!
					 		did_something = true;
					 		rval = (ReplacementString)project_keys.get(akey);
							String result = replaceAString(test,akey,rval); // get replacement, whether normal or special							
					 		ii.set(result); // overwrites current boilerplate string
						
					 	} // end if found one of the keys
					 } // end check all keys (may be more than one in a line)
			} // end pass all strings inside the list
			if (did_something) // debugging
			{
//System.out.println("   replacement flag seen");
				Iterator see = arr.iterator();
				while (see.hasNext())
				{
					// if debugging uncomment these 2 lines
					//System.out.println("R: " + 
					//	see.next());
					// if NOT debugging uncomment this
					see.next();
				}
			} // end if some key found
		
	} // end stringReplacer List

	/*
	 * test = full string within which we will perform replacement
	 * akey = string to be replaced
	 * rval = ReplacementString object, which contains the string
	 *    that will replace "akey", and a flag that indicates
	 *     whether special processing will occur.
	 */
	public String replaceAString(String test, String akey, ReplacementString rval)
	{
System.out.println("replaceAString: " + rval);
			if (rval.flag < 0)
			{
				// NORMAL replacement
				String result = test.replace(akey ,rval.rep); // simple replace
				// debug System.out.println("Replaced: " +
					// 				result);
			// caller decides to do this	ii.set(result); // overwrites current boilerplate string
			  return result; // modified string
			} // end normal string replacement
			else
			{
				// SPECIAL PROCESSING, not simple string replacement
					 			
				String res2 = specialReplacementProcessing(
					 			  rval);  // replace with returned string
			// caller does this	ii.set(res2);
				// debugSystem.out.println("Special Replaced: " +
				//	 				res2);
				return res2; // modified string
			} // end special processing
	} // end replaceastring

	/*
	 * given a single String, perform the replacement
	 * that we normally do with stringReplacer. It
	 * is designed to walk only String values inside
	 * a JSON array. This one can be called
	 * by anybody who has a String to be modified
	 */
	public String singleStringReplace(String test,
	Map project_keys)
	{
		String result = test; // working copy
		String akey = "";
		//boolean did_something = false; // keep flag, could be many replacements in a line
		Iterator inner = project_keys.keySet().iterator(); // all search keys
		while (inner.hasNext())
		{
			akey = (String)inner.next();
			if (result.indexOf(akey ) >= 0)
			{
				// HIT IT!
			//	did_something = true;
				ReplacementString rval = (ReplacementString)project_keys.get(akey);
				// replace back over for future testing
				result = replaceAString(test,akey,rval); // get replacement, whether normal or special							
			} // end if found one of the keys
		} // end check all keys (may be more than one in a line)
		return result; // either copy of original string, or modified version		
	} // end single string replace
	
	/*
	 * Get properties from the "options.json". User passes
	 * a String designating property. Child object (format-specific)
	 * provides the value (or default).
	 * 
	 * Anyone who wants to use this, OVERRIDE it
	 */
	public String getProperty(String name)
	{
		return null; // must override, if you want anything useful
	}

	public void modifyMetaData() throws Exception
	{
		/* 
		 * create and modify the Tree, and store
		 * the new version in GLOBAL project_keys
		 */
		project_keys = processMetaData(
			special_keys,
			null,false); // DONT let them run stringReplacer
			//null,true); // debugging on
//System.out.println("in csHTML, project_keys: " + project_keys);
		/*
		 * NOW, replace all project strings in the Lists
		 * that will be written to the output HTML.
		 */
		stringReplacer(front1,
			project_keys); // front matter
		stringReplacer(back1,
			project_keys); // end matter
	} // end modifyMetaData
	/*
	 * project-specific metadata processing
	 * 
	 * This is more generic code, removing a lot of 
	 * repetition from the descendents of this base class.
	 * 
	 * NOT everyone works the same, however, so we have
	 * to allow for stopping the process partway through.
	 * 
	 * We create and RETURN a modified TreeMap of metadata 
	 * (called project_keys)
	 * 
	 * We are passed an array of String called "special_keys". These keys 
	 * are unique to each Sink format. Each must match a major key,
	 * and they cause special processing. The KEY to the special
	 * processing is the POSITION in this array. 
	 * 
	 * In addition, we are passed an array of List items. Each 
	 * item in the array is a list of Strings that can be altered by
	 * the stringReplacer() method (see this object)
	 * We will execute that method on each List object instance.
	 * NOTE, this array can be null or empty!
	 * 
	 * NOTE: we use the global List "project_specific_metadata"
	 * in this method! 
	 * 
	 */            
    public TreeMap processMetaData(
		String [] special_keys,
		List [] to_process,
		boolean debug_it)  throws Exception
    {
    		/*
		 * process strings in Map, when particular metadata
	 	 * values are seen. Example PROJECT_AUTHOR would be
	 	 * replaced with "Bob Swanson" in the boilerplate.
	 	 *
	 	 * the global: project_specific_metadata
	 	 *
  		 * contains the key-value pairs
  		 *
	 	 * To do this, we walk through all objects of boilerplate
    		 * we have, replacing metadata keys with values
    		 * per that scheme.
    		 *
    		 *  Some replacements are more complex,
    		 * so the SpecialContent object we are within, will
    		 * have to do more than simple text replaces, from time to time.
    		 */

		if(debug_it)
		{
			// NOTE the project_keys Map is not yet built!
			//metaWalk(project_specific_metadata,false); 
		}
			// create key/value pairs before processing

		/*
		 * the following is created in this method
		 * populated in this method, and returned by this method
		 */
		TreeMap project_keys = new TreeMap();

		/* 
		 * the array must be even, each odd item is key, even
		 * value is the value. These are stored in the Map
		 */
		Object someobject = null;
		String key = "";
		List arr = project_specific_metadata;
		if (debug_it)
		{
			System.out.println("Project MetaData strings:" + arr.size());
		}
       	 	if (arr.size() %2 != 0)
		{
			throw new Exception("Problems with JSON, project metadata list is not even sized: " + arr.size());
		}
		for (int ii = 0 ; ii < arr.size() ; ii += 2)
		{
		someobject = arr.get(ii);
		if (!(someobject instanceof String))
		{
                	throw new Exception("Problem with project metadata at: " + ii + 
                                    " not string: " + someobject);
		}
		key = (String)someobject; // will be key
		someobject = arr.get(ii + 1);
		if (!(someobject instanceof String))
		{
			throw new Exception("Problem with project metadata at: " + (ii+1) + 
                                    " not string: " + someobject);
		}
		String replacement_value = (String)someobject;
 	           /*
 	            * look up the key to see if it requires
 	            * special processing. each SpecialCreator
 	            * child will have its own list of special processing
 	            * items. If found, we use the position in the
 	            * list as the indicator of what code to perform.
 	            */
		ReplacementString rs = null; // no match yet
		for (int inner = 0 ; inner < special_keys.length ; inner++)
		{
		// System.out.println("Testing: --" + special_keys[inner] + "-- with --" + key + "--"); // debugging, grump
			if (special_keys[inner].equals(key))
			{
 	            		rs = new ReplacementString(replacement_value,inner); // position
 	            	}
		}
		if (rs == null)
		{
			// not special
			rs = new ReplacementString(replacement_value,-1); // normal processing
		}
            	project_keys.put(key,rs); // populate the key value pairs
		} // end loop on the strings in project metadata
		if (debug_it)
		{
        		System.out.println("ProjectKeyValues: " + project_keys);  // debugging
		}

		/*
		 * at this time, we have project_keys available for substitution
		 * 
		 * we perform stringReplacer on any List that is
		 * passed to us.
		 * 
		 * NOTE: the list can be null or empty 
		 */
		if (to_process != null)
		{
			for (int inner = 0 ; inner < to_process.length ; inner++)
			{
// debug System.err.println("Going to process item: " + inner + ", List: " + to_process[inner]);
				stringReplacer(to_process[inner],project_keys); 
			}
		} // end if something to process through stringReplacer
		return project_keys; // return populated Tree to caller
	} // end process meta data


	public Properties getPropertiesFromJSON(String filename,
		String data_group) throws Exception
	{
		File input = new File(filename);
		ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
		Map<String,Object> userData = mapper.readValue(input,Map.class);
        
		// userData is a Map containing the named arrays
        
		/*
		 * a List of Strings that specify
		 * options. They
		 * are pairs, keyword, then value
		 */
		List optionsx = (List)userData.get(data_group); 
		Properties options = new Properties();
		/*
		 * options are keyword then value
		 */
		List arr = optionsx; // cast
		int count= arr.size();
		for (int inner = 0 ; inner < count ; inner+=2)
		{
			String key = (String)arr.get(inner);
			String value = (String)arr.get(inner+1);
			options.setProperty(key,value); // strings
		} // end loop on string pairs
		return options; // pass back Properties
	} // end get properties from JSON
	/*
	 * read JSON, using jackson, given the File object
	 * for the filename desired
	 */
	public static Map<String,Object> readJSON(File object_name,
		boolean debug_it) throws Exception
	{
		if (debug_it)
		{
			System.out.println("Getting JSON from: " + object_name);
		}
		ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
		Map<String,Object> userData = mapper.readValue(object_name,
			Map.class);
		return userData;
	} // end read JSON given File object of the JSON file
	/*
	 * read JSON, using jackson, given the object
	 * name to use to create filename desired
	 */
	public static Map<String,Object> readJSON(String object_name,
		boolean debug_it) throws Exception
	{
		String filename = object_name  + ".json"; // filename to read
		if (debug_it)
		{
			System.out.println("Getting JSON from: " + filename);
		}
		File input = new File(filename);
		ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
		Map<String,Object> userData = mapper.readValue(input,Map.class);
		return userData;
	} // end read JSON given object name
	/*
	 * this code is unique to this PROGRAM!
	 * each switch() position is based on the special_key[]
	 * position (global set for this program).
	 * 
	 * return a String that will be pushed back into the
	 * JSON structure, as a complete replacement
	 * for the original string in which the special key was
	 * found
	 */
	public String specialReplacementProcessing(ReplacementString rval)
	{
		StringBuffer result = new StringBuffer();
		switch (rval.flag)
		{
			case 0:
			{
			    	// "PROJECT_TEXT"
				/*
			    	 * "PROJECT_TEXT"
				 *
				 * multiple paragraphs, split with colon (:)
				 *
				 */
			    	String xx[] = rval.rep.split(":"); // single colon delimiter
        			for (int inner  = 0 ; inner < xx.length ; inner++)
        			{
        				result.append("<p>" + xx[inner] +
						"</p>"); // paragraph
						//first paragraph may need break(s) "<br/>");
				}  // end loop through all strings to be treated as separate lines         
			    	break;
			} // end 0 which is PROJECT_TEXT
			case 1:
			{
				/*
			    	 * "PROJECT_DESCRIPTION"
				 *
				 * Use the text, but add some more
				 */
				result.append(rval.rep + 
// boilerplate to add to any description DO NOT USE SINGLE QUOTES!
" This page is part of the Swanson web pages, which cover many of our interests, including: cruising, past RV travel, travel in general, photography, ships and boats, collecting, including postal history interests for Bob, our reading and music interests, as well as some links to web sites we often use to find out about the weather, political opinion, and investing.");
			    	break;
			} // end 1 which is PROJECT_DESCRIPTION
			case 2:
			{
			    	/*
				 *  "PROJECT_DATE"
				 */
			    Date today = new Date(); // use date/time within second
				DateFormat form = DateFormat.getDateTimeInstance(
					DateFormat.SHORT,
					DateFormat.SHORT);
				result.append(form.format(today));
			    	break;
			} // end 2 which is PROJECT_DATE
		} // end switch on special code segments
		return result.toString();		
	}	// end specialReplacementProcessing

	/*
	 * create a page of output from a list of Strings
	 */
    public static  void createAPage(PrintWriter pr, 
		List page_object,
		boolean close_at_end) throws Exception
    {
        Object someobject = null;
        Iterator ii = page_object.iterator();
        while (ii.hasNext())
        {	
            someobject =  ii.next();
            if (someobject instanceof String)
            {
                pr.println(someobject);
                continue; // done for now
            }
            //  now if there is a fall-through, the objects are somebody we don't know about
            throw new Exception("Problems with JSON: " + page_object);
            //throw new Exception("Problems with JSON inside: " + filename + ", object: " + someobject.getClass().getName());
        } // end write the content to the stream
        
	if (close_at_end)
	{
		pr.flush();
		pr.close();
	    }
    } // end create a page given filename
	
	/*
	 * container for the replacement string
	 * needed, when we are modifying the boilerplate
	 * from a project-agnostic, format-specific
	 * object. The key to the Map is the string
	 * we look for, and this object contains the
	 * string which we will substitute. 
	 *
	 * ALSO, we allow for substitution by special
	 * code. The type flag will be 0 or greater
	 * to indicate special processing. Each CreatSpecial
	 * child will have its own list of special items,
	 * and will have the code to make the special
	 * modification.
	 *
	 * If the flag is -1, no special processing takes
	 * place.
	 *
	 * Updated 4/23/2017
	 *
	 */
	public class ReplacementString
	{
	    public String rep; // replacement string (for key value)
	    public int flag; // -1, nothing special, 0 or greater points to code used in CreateSpecial object
		
	    /*
	     * simple constructor
	     */
	    public ReplacementString(String r,
		int f)
	    {
			rep = r;
			flag = f;
	    }
	
	    /*
	     * full toString() override
	     */
	    public String toString()
	    {
			if (flag < 0)
			{
			return "ReplacementString: " + rep + " (simple replacement)";
		     }
			else
			{
			return "ReplacementString: " + rep + " (special replacement, position: " +
			  flag + ")";
			}	
	    } // end tostring full override
	    
			  
	} // end replacement string container
} // end make web pages 
