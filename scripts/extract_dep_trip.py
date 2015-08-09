# extract triplet-based dependency pairs from a conll file.
# assumes google universal-treebank annotation scheme.
# zcat treebank.gz |python extract_deps.py |gzip - > deps.gz
import sys,re
from collections import defaultdict

vocab_file = sys.argv[1]
try:
   THR = int(sys.argv[2])
except IndexError: THR=100

lower=True

def read_conll(fh):
   root = (0,'*root*',-1,'rroot')
   tokens = [root]
   for line in fh:
      if lower: line = line.lower()
      tok = line.strip().split()
      if not tok:
         if len(tokens)>1: yield tokens
         tokens = [root]
      else:
          tokens.append((int(tok[0]), re.sub('^[,.;]$', '<punc>', re.sub('^\d+[.,]?\d*$', '<num>', tok[1])), int(tok[6]),tok[7]))
   if len(tokens) > 1:
      yield tokens

def read_vocab(fh):
   v = {}
   for line in fh:
      if lower: line = line.lower()
      line = line.strip().split()
      if len(line) != 2: continue
      if int(line[1]) >= THR:
         v[line[0]] = int(line[1])
   return v

vocab = set(read_vocab(file(vocab_file)).keys())
print >> sys.stderr,"vocab:",len(vocab)
for i,sent in enumerate(read_conll(sys.stdin)):
    if i % 100000 == 0: print >> sys.stderr,i
    for itok, tok in enumerate(sent):
        if itok == 0: continue
        ihead = tok[2]
        rel = tok[3]

        for offsetm in [-1,0,1]:
            locm = offsetm + itok
            if locm <= 0 or locm >= len(sent) or sent[locm][1] not in vocab:
                continue
            for offseth in [-1,0,1]:
                loch = offseth + ihead
				
                if loch < 0 or locm < 0 or loch == len(sent) or locm == len(sent) or sent[loch][1] == '<punc>' or sent[locm][1] == '<punc>':
                    continue

                print "_".join((sent[loch][1], str(offseth))),"_".join((str(offsetm), sent[locm][1]))

