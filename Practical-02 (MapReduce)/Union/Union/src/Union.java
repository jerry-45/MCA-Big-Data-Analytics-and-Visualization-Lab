import java.io.*;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class Union {
	public static class MultipleMaps extends Mapper<LongWritable, Text, Text, IntWritable>{
		private final static IntWritable one = new IntWritable();
		
		//creating object for Text
		
		private Text keyEmit = new Text();
		
		public void map(LongWritable k, Text value, Context context) throws IOException, InterruptedException{
			StringTokenizer itr = new StringTokenizer(value.toString());
			while(itr.hasMoreElements()){
				keyEmit.set(itr.nextToken());
				context.write(keyEmit, one);
			}
		}
	}
	
	public static class MultipleReducer extends Reducer<Text, IntWritable, Text, IntWritable>{
		
		private IntWritable result = new IntWritable();
		
		public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException{
			int sum = 0;
			
			for(IntWritable val : values){
				sum = sum + val.get();
			}
			
			result.set(sum);
			context.write(key, result);
		}
	}
	
	public static void main(String args[]) throws Exception{
		if(args.length != 3){
			System.err.println("Number of arguments must be 3...");
			System.exit(0);
		}
		
		Configuration c = new Configuration();
		String[] files = new GenericOptionsParser(c, args).getRemainingArgs();
		
		// proividing paths
		Path p1 = new Path(files[0]);
		Path p2 = new Path(files[1]);
		Path p3 = new Path(files[2]);
		
		FileSystem fs = FileSystem.get(c);
		
		if(fs.exists(p3)){
			fs.delete(p3, true);
		}
		
		Job job = Job.getInstance(c, "Union of 2 files.");
		job.setJarByClass(Union.class);
		
		MultipleInputs.addInputPath(job, p1, TextInputFormat.class, MultipleMaps.class);
		MultipleInputs.addInputPath(job, p2, TextInputFormat.class, MultipleMaps.class);
		
		job.setReducerClass(MultipleReducer.class);
		job.setCombinerClass(MultipleReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		
		FileOutputFormat.setOutputPath(job, p3);
		
		boolean success = job.waitForCompletion(true);
		System.exit(success ? 0:1);
	}
}