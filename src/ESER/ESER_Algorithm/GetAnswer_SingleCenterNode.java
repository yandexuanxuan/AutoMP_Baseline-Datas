package ESER.ESER_Algorithm;

import Graph.graph;
import JDBCUtils.JdbcUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class GetAnswer_SingleCenterNode {
	
	//h + k + Vector<EdgeNum> + Set<Nodes>
	
	public static class SatiPath
	{
		public Integer valH;
		public Vector<Integer> pathEdges;
		public Set<Integer> satiNodes;
		public Set<Integer> sampleNodes;
	}
	
	public static Integer LimH = 0;
	public static Integer TopK = 0;
	public static Integer LimS = 0;
	public static Integer CenterNode = 0;
	public static Integer Smooth = 0;
	public static Integer RelaxNumber = 0;
	public static Set<Integer> SampleNodes = new HashSet<Integer>();
	public static Set<Integer> CandidateNodes = new HashSet<Integer>();
	public static Vector<SatiPath> tmpSP = new Vector<SatiPath>();
	public static Vector<SatiPath> oneStep = new Vector<SatiPath>();
	public static Vector<SatiPath> SP = new Vector<SatiPath>();
	public static Set<Integer> walked = new HashSet<Integer>();
	public static Set<Integer> AlreadyChoose = new HashSet<Integer>();
	public static Vector<Integer> ResultNode = new Vector<Integer>();
	public static Vector<Double> ResultScore = new Vector<Double>();
	public static graph g;
	
	static {
        try {
            // 加载dbinfo.properties配置文件
        	InputStream in = JdbcUtil.class.getClassLoader()
                    .getResourceAsStream("ESER.properties");
            Properties properties = new Properties();
            properties.load(in);
            
            //多中心问题，从example发散的步数
            LimS = Integer.parseInt(properties.getProperty("LimitSearchSteps"));
            //SF的长度限制
            LimH = Integer.parseInt(properties.getProperty("LimitPhiSteps"));
            //返回多少个结果
            TopK = Integer.parseInt(properties.getProperty("TopK"));
            //是否采用平滑
			Smooth = Integer.parseInt(properties.getProperty("SmoothOrNot"));
            //找SF,选择中心时最多漏覆盖的Sample数
            RelaxNumber = Integer.parseInt(properties.getProperty("RelaxNumber"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	public static void clean()
	{
		walked.clear();
		AlreadyChoose.clear();
		ResultNode.clear();
		ResultScore.clear();
		CenterNode = 0;
		SampleNodes.clear();
		CandidateNodes.clear();
		SP.clear();tmpSP.clear();oneStep.clear();
	}
	
	/*
	private static Integer hash2Num(String hashnum)
	{
		Integer ret = 0;
		if(path.containsKey(hashnum)) ret = path.get(hashnum);
		else
		{
			ret = PathCnt ++;
			path.put(hashnum, PathCnt);
		}
		return ret;
	}
	*/
	
	private static void addInfo(Integer centernode, Vector<Integer> samplenodes)
	{
		CenterNode = centernode;
		SampleNodes.clear();
		for(Integer p : samplenodes)SampleNodes.add(p);
	}
	
	/*
	private static String vectorIntegerHash(Vector<Integer> v)
	{
		BigInteger sum = BigInteger.valueOf(0);
		BigInteger add = BigInteger.valueOf(1);
		BigInteger mul = BigInteger.valueOf(1000000);
		mul = mul.multiply(mul);
		for(Integer p : v)
		{
			BigInteger tmp = BigInteger.valueOf(p);
			tmp = tmp.multiply(add);
			add = add.multiply(mul);
			sum = sum.add(tmp);
		}
		return sum.toString();
	}
	*/
	
	private static void DFS(Integer node, Integer len, Vector<Integer> walkedges)
	{
		if(!g.G.containsKey(node))return;
		len ++;
		HashMap<Integer, List<Integer>> outedges = g.G.get(node);
		for(Map.Entry<Integer, List<Integer>> ent : outedges.entrySet())
		{
			Vector<Integer> tmpwalkedges = (Vector<Integer>) walkedges.clone();
			Integer edgenum = ent.getKey();
			List<Integer> outnodes = ent.getValue();
			tmpwalkedges.add(edgenum);
			SatiPath sp = new SatiPath();
			sp.pathEdges = new Vector<Integer>();
			sp.satiNodes = new HashSet<Integer>();
			sp.sampleNodes = new HashSet<Integer>();
			sp.valH = len; sp.pathEdges = (Vector<Integer>) tmpwalkedges.clone();
			for(Integer tonode : outnodes)
			{
				if(SampleNodes.contains(tonode)) sp.sampleNodes.add(tonode);
				sp.satiNodes.add(tonode);
			}
			tmpSP.add(sp);
			for(Integer tonode : outnodes)
				if(!walked.contains(tonode) && len<LimH)
				{
					walked.add(tonode);
					DFS(tonode, len, tmpwalkedges);
					walked.remove(tonode);
				}
		}
	}
	
	private static void find()
	{
		walked.add(CenterNode);
		Vector<Integer> pt = new Vector<Integer>();
		pt.clear();
		DFS(CenterNode, 0, pt);
	}
	
	private static void getSatisfiedPath()
	{
		Integer cnt = SampleNodes.size();
		SP.clear();
		for(SatiPath sp : tmpSP)
		{
			if(sp.valH == 1 || sp.sampleNodes.size() >= cnt - RelaxNumber)
			{
				SP.add(sp);
				for(Integer p : sp.satiNodes)
					if(!SampleNodes.contains(p))CandidateNodes.add(p);
			}
			if(sp.valH == 1)oneStep.add(sp);
		}
	}
	
	private static Integer setCap(Set<Integer> s1, Set<Integer> s2)
	{
		Integer cnt = 0;
		for(Integer p : s1)if(s2.contains(p))cnt ++;
		return cnt;
	}
	
	private static void calculateScore()
	{
		//System.out.println(CandidateNodes.size());
		Vector<Double> sc = new Vector<Double>();
		Vector<Double> SPScore = new Vector<Double>();
		SPScore.clear();
		for(SatiPath sp : SP)
		{
			double d = 0, r = 1.0;
			sc.clear();
			for(SatiPath spp : oneStep) sc.add((double)setCap(sp.satiNodes, spp.satiNodes) / (double)sp.satiNodes.size());
			d = 1.0 / (double)(sp.satiNodes.size());
			r = 1.0;
			for(Integer samplenodes : SampleNodes)
			{
				if(sp.sampleNodes.contains(samplenodes))continue;
				double s1 = 0, s2 = 0;
				if(Smooth > 0) { s1 = 1; s2 = 1; }
				for(Integer i=0; i<oneStep.size(); ++i)
				{
					SatiPath spp = oneStep.elementAt(i);
					double tmp = sc.elementAt(i);
					if(spp.sampleNodes.contains(samplenodes))s1 += tmp;
					s2 += tmp;
				}
				if(sp.valH > 1) {s1 += 1.0; s2 += 1.0;}
				r *= (s1 / s2);
				//if(s1 > 0)r *= (s1 / s2);
			}
			SPScore.add(d * r);
		}
		/*
		for(Integer i=0; i<SP.size(); ++i)
		{
			SatiPath sp = SP.elementAt(i);
			System.out.print(sp.valH + "  " + sp.sampleNodes.size() + "  " + sp.satiNodes.size() + "   ");
			System.out.println(SPScore.elementAt(i));
		}
		*/
		for(Integer nodenum : CandidateNodes)
		{
			double sum = 0;
			for(Integer i=0; i<SP.size(); ++i)
			{
				SatiPath sp = SP.elementAt(i);
				if(sp.satiNodes.contains(nodenum))sum += SPScore.elementAt(i);
			}
			ResultNode.add(nodenum);
			ResultScore.add(sum);
		}
		/*
		for(Integer i=0; i<ResultNode.size(); ++i)
		{
			System.out.print(ResultNode.elementAt(i) + "  ");
			System.out.println(ResultScore.elementAt(i));
		}
		*/
	}
	
	public static Vector<Integer> findanswer(Integer centernode, Vector<Integer> relativenode, graph gg)
	{
		Vector<Integer> ret = new Vector<Integer>();
		ret.clear();g = gg;
		addInfo(centernode, relativenode);
		find();getSatisfiedPath();calculateScore();
		Integer topK = TopK;
		while(topK > 0)
		{
			-- topK;
			double a1 = -1;
			Integer a2 = 0;
			for(Integer i=0; i<ResultNode.size(); ++i)
			{
				Integer nodenum = ResultNode.elementAt(i);
				if(AlreadyChoose.contains(nodenum))continue;
				double score = ResultScore.elementAt(i);
				if(score > a1)
				{
					a1 = score;
					a2 = nodenum;
				}
			}
			if(a1 > 0)
			{
				ret.add(a2);
				AlreadyChoose.add(a2);
			}else ret.add(0);
		}
		return ret;
	}
}
