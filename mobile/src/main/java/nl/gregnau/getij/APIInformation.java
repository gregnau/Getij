package nl.gregnau.getij;

import java.util.HashMap;
import java.util.Map;

/*Class that holds information for our API call*/
public class APIInformation {

    private String APIEndpoint;
    private Map<String, String> APIArguments = new HashMap<>();
    private String APIUrl;

    //endpoint (main url to the target API)
    public void setAPIEndpoint(String endpoint){
        this.APIEndpoint = endpoint;
    }
    public String getAPIEndpoint(){
        return APIEndpoint;
    }

    //set a single API argument to the arg list
    public void setAPIArgument(String key, String value){
        this.APIArguments.put(key, value);
    }

    //gets a single API argument from the arg list
    public String getAPIArgument(String key){
        String value = APIArguments.get(key);
        return value;
    }

    //set a bunch of API arguments to the arg list
    public void setAPIArguments(HashMap<String, String> arguments){
        this.APIArguments = arguments;
    }

    //gets all the API arguments as a HashMap
    public Map<String, String> getAPIArguments(){
        return APIArguments;
    }


    //creates the final API url based on endpoint, key and arguments
    public void setAPIUrl(){
        StringBuilder builder = new StringBuilder();

        builder.append(this.APIEndpoint);

        //loop through all arguments
        Map<String,String> arguments = this.getAPIArguments();
        if(arguments.size() != 0){

            Integer counter = 1;
            builder.append("?");
            for ( Map.Entry<String, String> entry : arguments.entrySet()){
                String key = entry.getKey();
                String value = entry.getValue();

                //check for empty keys or values (some arguments don't need values passed)
                if(value.isEmpty() && !key.isEmpty()){
                    builder.append(key);
                }else{
                    builder.append(key + "=" + value);
                }

                if(counter != arguments.size()){
                    builder.append("&");
                }

                counter++;
            }
        }

        this.APIUrl = builder.toString();
    }

    //gets the final API url to call
    public String getAPIUrl(){
        return this.APIUrl;
    }
}
