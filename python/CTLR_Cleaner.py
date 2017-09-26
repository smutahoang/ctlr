from os import path
import os
import sys
import csv
import time
import datetime
import urllib
import json
import re
import datetime
import math
from datetime import datetime, timedelta
import random
import operator

def test_json(jsonFile,output):
	count = 0
	with open(output, 'a+',encoding='utf-8') as fout:
		for line in open(jsonFile,encoding='utf-8'):
			fout.writelines(line)
			if count>10:
				break
			count+=1

def remove_url(inputStr):
	outputStr = ''
	if "http" in inputStr: 
		outputStr = re.sub(r"https\S+", "", inputStr)
	if "http" in inputStr: 
		outputStr = re.sub(r"http\S+", "", inputStr)	
	return outputStr

def remove_non_alpnum(inputStr):
	regex = re.compile('([^\s\w]|_)+')
	outputStr = regex.sub('',inputStr)
	return outputStr

def remove_non_alpa(inputStr):
	regex = re.compile('[^a-zA-Z]')
	outputStr = regex.sub(' ',inputStr)
	outputStr = re.sub(' +',' ',outputStr)
	return outputStr

def get_common_words(inputFile, outputFile, topK):
	lwords = {}
	with open(inputFile, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			text = row[1].strip()
			words = text.split(' ')
			for w in words:
				w = w.strip()
				if w !='':
					if w in lwords:
						value = lwords[w]+1
						lwords[w] = value
					else:
						lwords[w] = 1
	sorted_words = sorted(lwords.items(), key=operator.itemgetter(1), reverse=True)
	k=0
	with open(outputFile, 'a+',encoding='utf-8') as fout:				
		for key in lwords:
			if (k<=topK):
				fout.writelines(key+",")
			k = k+1

def get_rare_words(inputFile, outputFile, minFreq):
	lwords = {}
	with open(inputFile, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			text = row[1].strip()
			words = text.split(' ')
			for w in words:
				w = w.strip()
				if w !='':
					if w in lwords:
						value = lwords[w]+1
						lwords[w] = value
					else:
						lwords[w] = 1

	with open(outputFile, 'a+',encoding='utf-8') as fout:				
		for key in lwords:
			if (int(lwords[key])<=minFreq):
				fout.writelines(key+",")

def get_vocabulary(inputFile,outputFile):
	#lWords =[]
	sWords = set()
	line = 0
	with open(inputFile,encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			text = row[1].strip()
			words = text.split()
			#lWords = lWords + set(text.split())
			sWords = sWords | set(text.split())
			print(line)
			line += 1
	#sWords = set(lWords)		


			#for w in words:
			#	if w in ls_Words:
			#		"do nothing"
			#	else:
			#		ls_Words.append(w)
			
	with open(outputFile, 'a+',encoding='utf-8') as fout:
		index = 0;				
		for w in sWords:
			fout.writelines(str(index)+","+str(w)+"\n")
			index+=1

def update_stopwords(i_currentStopwords,i_newStopwords,o_stopwordFile):
	file = open(i_currentStopwords, 'r')
	cStopwords = file.readline().lower()
	lCStopwords = cStopwords.split(',')

	file = open(i_newStopwords, 'r')
	nStopwords = file.readline().lower()
	lNStopwords = nStopwords.split(',')	

	lMergedStopwrds = lCStopwords + lNStopwords
	sStopwwords = set(lMergedStopwrds)

	with open(o_stopwordFile, 'a+') as fout:
		for w in sStopwwords:
			fout.writelines(w+",")

def remove_stopwords(i_stopwordFile,i_posts,o_posts):
	file = open(i_stopwordFile, 'r')
	stopwords = file.readline().lower()
	lStopwords = stopwords.split(',')
	sStopwords = set(lStopwords)
	nLine=0
	with open(o_posts, 'a+',encoding='utf-8') as fout:
		with open(i_posts,encoding='utf-8') as f:
			reader = csv.reader(f, delimiter=',')
			for row in reader:
				nLine +=1
				uid = row[0]
				text = row[1].lower().strip()
				text = remove_non_alpa(text).strip()
				nText =""
				words = text.split(' ')
				sWords = set(words)
				cleanWords = sWords - sStopwords
				if len(cleanWords) != 0:
					fout.writelines(str(uid)+","+' '.join(cleanWords)+"\n")
				print(nLine)

def retrieve_twt_relationship_stats_JSON(i_JSONFile, i_minFollower, i_minFollowee, o_UserFollowerCounts, o_UserFolloweeCounts):
	lUsers = []
	dFollowerCounts = {}
	dFolloweeCounts = {}
	with open(i_JSONFile) as f:
		content  = f.readlines()
		for json_line in content:
			data = json.loads(json_line)
			src_uid = data['_source']['user_id']
			lFollowees = data['_source']['followees_id']
			for des_uid in lFollowees:
				#Update Followees Count
				if src_uid in dFolloweeCounts:
					dFolloweeCounts[src_uid] = dFolloweeCounts[src_uid] + 1
				else:
					dFolloweeCounts[src_uid] = 1
				#Update Followers Count
				if des_uid in dFollowerCounts:
					dFollowerCounts[des_uid] = dFollowerCounts[des_uid] + 1
				else:
					dFollowerCounts[des_uid] = 1


	with open(o_UserFollowerCounts, 'a+') as fout:
		for uid in  dFollowerCounts:
			if dFollowerCounts[uid] >= i_minFollower:
				lUsers.append(uid)
				fout.writelines(str(uid)+","+ str(dFollowerCounts[uid])+"\n")

	with open(o_UserFolloweeCounts, 'a+') as fout:
		for uid in  dFolloweeCounts:
			if dFolloweeCounts[uid] >= i_minFollowee:
				lUsers.append(uid)
				fout.writelines(str(uid)+","+ str(dFolloweeCounts[uid])+"\n")

def retrieve_twt_posts_JSON(i_JSONfile, i_userFile, i_maxPost, o_userPosts, o_userPostsCount):
	dUsers = {}
	for uid in open(i_userFile):
		uid = long(uid)
		dUsers[uid] = 0
		
	with open(o_userPosts, 'a+') as fout:
		for json_line in open(i_JSONfile):
			data = json.loads(json_line)
			uid = long(data['user']['id'])
			if uid in dUsers:
				if dUsers[uid] <= i_maxPost:
					text = data['text'].lower().strip()
					text = text.replace('\n',' ')
					text = text.replace('\r',' ')
					text = remove_url(text)
					text = remove_non_alpnum(text).strip()
					if text.strip() != '':
						fout.writelines(str(uid) + ',' + text.strip() + '\n')
						dUsers[uid] =  dUsers[uid] + 1

	with open(o_userPostsCount, 'a+') as fout:
		for uid in dUsers:
			fout.writelines(str(uid) + ',' + str(dUsers[uid]) + '\n')

def retrieve_twt_relationship_JSON(i_JSONFile, i_users, o_relationships):
	lUsers = []

	with open(i_users) as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = row[0]
			uid = str(uid)
			lUsers.append(uid)

	with open(o_relationships, 'a+') as fout:
		with open(i_JSONFile) as f:
			content  = f.readlines()
			for json_line in content:
				data = json.loads(json_line)
				src_uid = data['_source']['user_id']
				src_uid = str(src_uid)
				lFollowees = data['_source']['followees_id']
				if src_uid in lUsers:
					for des_uid in lFollowees:
						des_uid = str(des_uid)
						if des_uid in lUsers:
							fout.writelines(src_uid+','+des_uid+ '\n')

def retrieve_ins_users_JSON(i_JSONFile, o_Users):
	lUsers = []
	
	for json_line in open(i_JSONFile):
		data = json.loads(json_line)
		uid = data['_source']['insta_user_id']
		lUsers.append(uid)


	sUsers = set(lUsers)
		
	with open(o_Users, 'a+') as fout:
		for uid in  sUsers:
			fout.writelines(str(uid)+"\n")

def retrieve_ins_relationship_stats_JSON(i_JSONFile, i_users, o_relationships, o_followerStats, o_followeeStats):
	lUsers = []
	dFollowers = {}
	dFollowees = {}

	with open(i_users, encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = str(row[0])
			lUsers.append(uid)
			dFollowers[uid] = 0
			dFollowees[uid] = 0

	numProcess = 0
	sUsers = set(lUsers)
	with open(o_relationships, 'a+') as fout:
		for json_line in open(i_JSONFile, encoding='utf-8'):
			data = json.loads(json_line)
			uid = str(data['_source']['insta_user_id'])
			lFollowers = data['_source']['followers_id']
			lFollowees = data['_source']['followees_id']
			sFollowers = set(lFollowers)
			sFollowees = set(lFollowees)
			sValidFollowers = sUsers & sFollowers
			sValidFollowees = sUsers & sFollowees
			numProcess += 1
			print('uid:'+uid+' '+ str(numProcess))
			if uid in lUsers: 
				for src_uid in sValidFollowers:
					dFollowers[uid] = dFollowers[uid] + 1
					dFollowees[src_uid] = dFollowees[src_uid] + 1
					fout.writelines(src_uid+','+uid+"\n")
				for des_uid in sValidFollowees:
					dFollowers[des_uid] = dFollowers[des_uid] + 1
					dFollowees[uid] = dFollowees[uid] + 1
					fout.writelines(uid+','+des_uid+"\n")
		
	with open(o_followerStats, 'a+') as fout:
		for uid in dFollowers:
			fout.writelines(str(uid)+','+str(dFollowers[uid])+"\n")

	with open(o_followeeStats, 'a+') as fout:
		for uid in dFollowees:
			fout.writelines(str(uid)+','+str(dFollowees[uid])+"\n")

def retrieve_ins_posts_JSON(i_JSONFile, i_users, o_posts, o_postCounts):
	dPosts = {}
	with open(i_users, encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:	
			uid = str(row[0])
			dPosts[uid] = 0

	with open(o_posts, 'a+', encoding='utf-8') as fout:
		for json_line in open(i_JSONFile, encoding='utf-8'):
			data = json.loads(json_line)
			if 'caption' in data['_source']:
				uid  = str(data['_source']['caption']['from']['id'])
				text = str(data['_source']['caption']['text'])
				text = text.lower()
				text = text.replace('\n',' ')
				text = text.replace('\r',' ')
				text = remove_url(text)
				text = remove_non_alpa(text).strip()
				text = text.strip()
				if uid in dPosts:
					if text != '':
						fout.writelines(str(uid) + ',' + text + '\n')
						dPosts[uid] =  dPosts[uid] + 1

	with open(o_postCounts, 'a+') as fout:
		for uid in dPosts:
			fout.writelines(str(uid)+','+str(dPosts[uid])+"\n")

def retrieve_ins_posts_all_users_JSON(i_JSONFile, o_posts, o_postCounts):
	dPosts = {}
	with open(o_posts, 'a+', encoding='utf-8') as fout:
		for json_line in open(i_JSONFile, encoding='utf-8'):
			data = json.loads(json_line)
			if 'caption' in data['_source']:
				uid  = str(data['_source']['caption']['from']['id'])
				text = str(data['_source']['caption']['text'])
				text = text.lower()
				text = text.replace('\n',' ')
				text = text.replace('\r',' ')
				text = remove_url(text)
				text = remove_non_alpnum(text).strip()
				text = text.strip()
				if text != '':
					fout.writelines(str(uid) + ',' + text + '\n')
					if uid in dPosts:
						dPosts[uid] =  dPosts[uid] + 1
					else:
						dPosts[uid] =  1

	with open(o_postCounts, 'a+') as fout:
		for uid in dPosts:
			fout.writelines(str(uid)+','+str(dPosts[uid])+"\n")

def remove_duplicate_relationships(i_relationships,o_relationships):
	lRelationships = []
	with open(i_relationships, newline='') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			src_user_id = row[0]
			des_user_id = row[1]
			relationship = str(src_user_id)+','+str(des_user_id)
			lRelationships.append(relationship)
	sRelationships = set(lRelationships)
	with open(o_relationships, 'a+') as fout:
		for line in sRelationships:
			fout.writelines(line+"\n")

def remove_self_link(i_relationships, o_relationships):
	with open(o_relationships, 'a+') as fout:
		with open(i_relationships, newline='') as f:
			reader = csv.reader(f, delimiter=',')
			for row in reader:
				src_author = str(row[0])
				des_author = str(row[1])
				if src_author != des_author:
					fout.writelines(src_author+','+ des_author +'\n')	

def remove_empty_posts(i_posts,o_posts):
	with open(o_posts, 'a+') as fout:
		with open(i_posts, newline='') as f:
			reader = csv.reader(f, delimiter=',')
			for row in reader:
				uid = str(row[0])
				text = str(row[1]).strip()
				if text != '':
					fout.writelines(uid+','+text+"\n")

def filter_posts_by_users(i_posts, i_users, o_posts):
	lUsers = []
	with open(i_users, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = str(row[0])
			lUsers.append(uid)
	with open(o_posts, 'a+',encoding='utf-8') as fout:
		with open(i_posts, newline='',encoding='utf-8') as f:
			reader = csv.reader(f, delimiter=',')
			for row in reader:
				uid = str(row[0])
				text = remove_non_alpa(str(row[1])).strip()
				if uid in lUsers:
					if (text != ''):
						fout.writelines(uid+','+text+ '\n')

def filter_relationships_by_users(i_relationships, i_users, o_relationships):
	lUsers = []
	with open(i_users, newline='') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = str(row[0])
			lUsers.append(uid)
	with open(o_relationships, 'a+') as fout:
		with open(i_relationships, newline='') as f:
			reader = csv.reader(f, delimiter=',')
			for row in reader:
				src_uid = str(row[0])
				des_uid = str(row[1])
				if src_uid in lUsers and des_uid in lUsers:
					fout.writelines(src_uid+','+des_uid+ '\n')

def retrieve_posts_stats(i_posts, i_users, o_postStats):
	dPosts = {}
	with open(i_users, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = str(row[0])
			dPosts[uid] = 0
	with open(i_posts, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = str(row[0])
			text = row[1]
			if uid in dPosts:
				dPosts[uid] = dPosts[uid] + 1
	with open(o_postStats, 'a+',encoding='utf-8') as fout:
		for uid in dPosts:
			fout.writelines(str(uid)+','+str(dPosts[uid])+"\n")

def retrieve_relationships_stats(i_relationships, i_users, o_relationshipStats):
	dFollowers = {}
	dFollowees = {}
	with open(i_users, newline='') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = str(row[0])
			dFollowers[uid] = 0
			dFollowees[uid] = 0
	with open(i_relationships, newline='') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			src_uid = str(row[0])
			des_uid = str(row[1])
			if des_uid in dFollowers:
				dFollowers[des_uid] = dFollowers[des_uid] + 1
			if src_uid in dFollowees:
				dFollowees[src_uid] = dFollowees[src_uid] + 1
	with open(o_relationshipStats, 'a+') as fout:
		for uid in dFollowees:
			combined = dFollowers[uid] + dFollowees[uid]
			fout.writelines(str(uid)+','+str(dFollowers[uid])+','+str(dFollowees[uid])+','+str(combined)+"\n")

def convert_post_word_to_index(i_vocabs, i_posts, o_indexedPosts):
	lWords = {}
	with open(i_vocabs,encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',',)
		for row in reader:
			index = row[0]
			word = row[1]
			lWords[word] = index
	postId = 0
	with open(o_indexedPosts, 'a+',encoding='utf-8') as fout:
		with open (i_posts,encoding='utf-8') as f:	
			reader = csv.reader(f, delimiter=",")
			for row in reader:
				userId = row[0]
				text = row[1].strip()
				words = text.split()
				indices = ""
				for w in words:
					indices += str(lWords[w]) + " "
				indices = indices.strip()
				fout.writelines(str(postId)+","+str(userId)+","+str(indices)+"\n")
				postId += 1

def label_batch_posts(i_posts,o_labeledPosts):
	lUserPosts = {}
	lUserTrains = {}
	with open(i_posts, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			userId = row[1]
			if userId in lUserPosts:
				value  = lUserPosts[userId] + 1
				lUserPosts[userId] = value
			else:
				lUserPosts[userId] = 1
	#Compute the number of training post for each user.  we label 80% of the user post for training (1)
	for key in lUserPosts:
			nTrain = math.ceil(float(lUserPosts[key])*0.8) 	
			lUserTrains[key] = nTrain
	with open(o_labeledPosts, 'a+',encoding='utf-8') as fout:
		with open(i_posts, newline='',encoding='utf-8') as f:
			reader = csv.reader(f, delimiter=',')
			for row in reader:
				postId = row[0]
				userId = row[1]
				content = row[2]
				if lUserTrains[userId]>0:
					label = 1
					newCount = lUserTrains[userId] - 1
					lUserTrains[userId] = newCount
				else:
					label = 0
				fout.writelines(str(postId)+','+str(userId)+','+str(content)+','+str(label)+"\n")			

def generate_non_relationships(i_users,i_relationships,o_nonRelationships):
	lUserFollowerCounts = {}
	lUserFollowingCounts = {}
	lUserFollowers = {}	
	lUserFollowings = {}
	lUsers = []
	lNonRelationships = []

	with open(i_users, newline='') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = str(row[0])
			lUsers.append(uid)
			lUserFollowerCounts[uid] = 0
			lUserFollowingCounts[uid] = 0
			lUserFollowers[uid] = '' 	
			lUserFollowings[uid] = ''

	#Load relationship file
	with open(i_relationships, newline='') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			src_user_id = str(row[0])
			des_user_id = str(row[1])
			
			lUserFollowingCounts[src_user_id] = lUserFollowingCounts[src_user_id] + 1
			lUserFollowings[src_user_id] = lUserFollowings[src_user_id] + ' ' + des_user_id
			lUserFollowerCounts[des_user_id] = lUserFollowerCounts[des_user_id] + 1
			lUserFollowers[des_user_id] = lUserFollowers[des_user_id] + ' ' + src_user_id
	
	count = 0		
	for userId in lUsers:
		count += 1
		print('Process User ' + userId + ',' + str(count))
		lPotentialNonFollowers = list(lUsers)
		lPotentialNonFollowings = list(lUsers)
		#Check that the user has followers		
		if userId in lUserFollowers: 
			#Generate potential user's potential non follower
			followers = lUserFollowers[userId].strip().split(' ')
			for follower in followers:
				lPotentialNonFollowers.remove(follower)
			
			#Set the number of non followers
			nNonFollowers =0;
			if ((lUserFollowerCounts[userId] * 5)>len(lPotentialNonFollowers)):
				nNonFollowers = len(lPotentialNonFollowers) 
			else:
				nNonFollowers = lUserFollowerCounts[userId] * 5	

			#Randomly pick a non followers
			while nNonFollowers > 0:
				nonFollower = random.choice(lPotentialNonFollowers)
				nonRelatinonship = nonFollower + ',' + userId
				if nonRelatinonship not in lNonRelationships:
					lNonRelationships.append(nonRelatinonship)
				lPotentialNonFollowers.remove(nonFollower)
				nNonFollowers -= 1 
		
		if userId in lUserFollowings:
			#Generate potential user's potential non following
			followings = lUserFollowings[userId].strip().split(' ')
			for following in followings:
				lPotentialNonFollowings.remove(following)	

			#Set the number of non followings
			nNonFollowings =0;
			if (lUserFollowingCounts[userId] * 5)>len(lPotentialNonFollowings):
				nNonFollowings = len(lPotentialNonFollowings) 
			else:
				nNonFollowings = lUserFollowingCounts[userId] * 5

			#Randomly pick a non followings
			while nNonFollowings > 0:
				nonFollowing = random.choice(lPotentialNonFollowings)
				nonRelatinonship = str(userId) + ',' + str(nonFollowing)
				if nonRelatinonship not in lNonRelationships:
					lNonRelationships.append(nonRelatinonship)
				lPotentialNonFollowings.remove(nonFollowing)
				nNonFollowings -= 1

	with open(o_nonRelationships, 'a+') as fout:
		for nonRelatinonship in lNonRelationships:
			fout.writelines(nonRelatinonship+"\n")

def generate_non_relationships_random(i_users,i_relationships,o_nonRelationships):
	KeepGoing = True
	lUsers = []
	sRelationships = set() 
	sNonRelationships = set()
	with open(i_users, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = str(row[0])
			lUsers.append(uid)
	with open(i_relationships, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			src_uid = str(row[0])
			des_uid = str(row[1])
			relationship = src_uid+','+des_uid
			sRelationships.add(relationship)
	
	relationshipsCount = len(sRelationships)
	nonRelationshipsCount = relationshipsCount * 5

	if nonRelationshipsCount > (len(lUsers)**2 - relationshipsCount):
		nonRelationshipsCount = (len(lUsers)**2 - relationshipsCount)

	numProcess = 0
	balance = nonRelationshipsCount
	while KeepGoing:
		print('processing '+str(balance))
		for x in range (0,balance):
			non_src_uid = random.choice(lUsers)
			allowChoices = list(lUsers)
			allowChoices.remove(non_src_uid)
			non_des_uid = random.choice(allowChoices)
			nonRelationship = non_src_uid+','+non_des_uid
			sNonRelationships.add(nonRelationship)
			numProcess +=1
			print(numProcess)
		
		sNonRelationships = sNonRelationships - sRelationships	
		balance = nonRelationshipsCount - len(sNonRelationships)
		if balance == 0:
			KeepGoing = False

	with open(o_nonRelationships, 'a+',encoding='utf-8') as fout:
		for nonRelatinonship in sNonRelationships:
			fout.writelines(nonRelatinonship+"\n")

def generate_non_relationships_followee(i_users,i_relationships,o_nonRelationships):
	lUsers = []
	sRelationships = set() 
	dFollowees = {}
	sNonRelationships = set()
	with open(i_users, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = str(row[0])
			lUsers.append(uid)
	with open(i_relationships, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			src_uid = str(row[0])
			des_uid = str(row[1])
			relationship = src_uid+','+des_uid
			sRelationships.add(relationship)
			if src_uid not in dFollowees:
				dFollowees[src_uid] = des_uid
			else:
				dFollowees[src_uid] = dFollowees[src_uid] + ' ' + des_uid			
	for uid in lUsers:
		lUFollowees = dFollowees[uid].split(' ')
		for vid in lUFollowees:
			lVFollowees = dFollowees[vid].split(' ')
			for wid in lVFollowees:
				nonRelationship = uid+','+ wid
				sNonRelationships.add(nonRelationship)
	sNonRelationships = sNonRelationships - sRelationships
	with open(o_nonRelationships, 'a+',encoding='utf-8') as fout:
		for nonRelatinonship in sNonRelationships:
			fout.writelines(nonRelatinonship+"\n")

def generate_non_relationships_popular(i_users,i_maxNonLinks,i_relationships,o_nonRelationships):
	lUsers = []
	sRelationships = set() 
	dFollowerCount = {}
	sNonRelationships = set()
	with open(i_users, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = str(row[0])
			lUsers.append(uid)
	with open(i_relationships, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			src_uid = str(row[0])
			des_uid = str(row[1])
			relationship = src_uid+','+des_uid
			sRelationships.add(relationship)
			if des_uid not in dFollowerCount:
				dFollowerCount[des_uid] = 1
			else:
				dFollowerCount[des_uid] = dFollowerCount[des_uid] + 1	
	sorted_dFollowerCount = sorted(dFollowerCount.items(), key=operator.itemgetter(1),reverse=True)
	for uid in lUsers:
		currCount = 0;
		for vid in sorted_dFollowerCount:
			if currCount < i_maxNonLinks:
				nonRelationship = uid +','+ str(vid[0])
				if nonRelationship not in sNonRelationships:
					sNonRelationships.add(nonRelationship)
					currCount += 1
	sNonRelationships = sNonRelationships - sRelationships
	with open(o_nonRelationships, 'a+',encoding='utf-8') as fout:
		for nonRelatinonship in sNonRelationships:
			fout.writelines(nonRelatinonship+"\n")

def generate_non_relationships_combined(i_users, iNonFollowerWeight,iNonFolloweeWeight, i_nonRelationshipPopular, i_nonRelationshipFollowee, i_relationships, o_nonRelationships):
	lUsers = []
	dFollowees = {}
	dFolloweeCounts = {}
	dFollowerCounts = {}
	dNonFolloweeCounts = {}
	dNonFollowerCounts = {}
	dMaxNonFolloweeCounts = {}
	dMaxNonFollowerCounts = {}
	dNonFollowees_Priority = {}
	sNonRelationships = set()
	with open(i_users, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = str(row[0])
			lUsers.append(uid)
			dFolloweeCounts[uid] = 0
			dFollowerCounts[uid] = 0
			dNonFolloweeCounts[uid] = 0
			dNonFollowerCounts[uid] = 0
			dNonFollowees_Priority[uid] = ''
			dFollowees[uid] = ''
	
	with open(i_relationships, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			src_uid = str(row[0])
			des_uid = str(row[1])
			dFollowees[src_uid] = dFollowees[src_uid] + ' ' + des_uid
			dFolloweeCounts[src_uid] = dFolloweeCounts[src_uid] + 1
			dFollowerCounts[des_uid] = dFollowerCounts[des_uid] + 1
			
	with open(i_nonRelationshipPopular, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			src_uid = str(row[0])
			des_uid = str(row[1])
			dNonFollowees_Priority[src_uid] = dNonFollowees_Priority[src_uid] + ' ' + des_uid

	with open(i_nonRelationshipFollowee, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			src_uid = str(row[0])
			des_uid = str(row[1])
			dNonFollowees_Priority[src_uid] = dNonFollowees_Priority[src_uid] + ' ' + des_uid

	for uid in lUsers:
		dMaxNonFolloweeCounts[uid] = int((len(lUsers) - dFolloweeCounts[uid]) * iNonFolloweeWeight)
		dMaxNonFollowerCounts[uid] = int((len(lUsers) - dFollowerCounts[uid]) * iNonFollowerWeight)
		
	sUsers = set(lUsers)
	for uid in lUsers:
		sNonFollowees_Priority = set(dNonFollowees_Priority[uid].strip().split(' '))
		sFollowees = set(dFollowees[uid].strip().split(' '))
		sNonFollowees_Rest = sUsers - (sFollowees | sNonFollowees_Priority)
		#print(uid)
		#print(len(sFollowees))
		#print(len(sNonFollowees_Priority))
		#print(len(sNonFollowees_Rest))
		#print(dMaxNonFolloweeCounts[uid])
		keepGoing = True
		while keepGoing:
			print(str(uid) + str(dNonFolloweeCounts[uid]))
			if len(sNonFollowees_Priority) > 0:
				lVid = random.sample(sNonFollowees_Priority,1)
				vid = lVid[0]
				if dNonFollowerCounts[vid] < dMaxNonFollowerCounts[vid]:
					nonRelationship = uid+','+ vid
					if nonRelationship not in sNonRelationships:
						sNonRelationships.add(nonRelationship)
						dNonFollowerCounts[vid] = dNonFollowerCounts[vid] +1
						dNonFolloweeCounts[uid] = dNonFolloweeCounts[uid] +1
						sNonFollowees_Priority.remove(vid)
					else:
						sNonFollowees_Priority.remove(vid)
				else:
					sNonFollowees_Priority.remove(vid)
			else:
				lVid = random.sample(sNonFollowees_Rest,1)
				vid = lVid[0]
				if dNonFollowerCounts[vid] < dMaxNonFollowerCounts[vid]:
					nonRelationship = uid+','+ vid
					if nonRelationship not in sNonRelationships:
						sNonRelationships.add(nonRelationship)
						dNonFollowerCounts[vid] = dNonFollowerCounts[vid] +1
						dNonFolloweeCounts[uid] = dNonFolloweeCounts[uid] +1
						sNonFollowees_Rest.remove(vid)
					else:
						sNonFollowees_Rest.remove(vid)
				else:
					sNonFollowees_Rest.remove(vid)
			if dNonFolloweeCounts[uid] >= dMaxNonFolloweeCounts[uid] or len(sNonFollowees_Rest)==0:
				keepGoing = False
		
	with open(o_nonRelationships, 'a+',encoding='utf-8') as fout:
		for nonRelatinonship in sNonRelationships:
			fout.writelines(nonRelatinonship+"\n")	

def label_batch_relationships(i_relationships,i_LabeledRelationships):
	lUserRelationships = {}
	lUserTrains = {}
	
	#Get the relationship counts for each user
	with open(i_relationships, newline='') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			src_user_id= row[0]
			if src_user_id in lUserRelationships:
				value  = lUserRelationships[src_user_id] + 1
				lUserRelationships[src_user_id] = value
			else:
				lUserRelationships[src_user_id] = 1

	#Compute the number of training post for each user.  we label 80% of the user relationship for training (1)
	for key in lUserRelationships:
			nTrain = math.ceil(float(lUserRelationships[key])*0.8) 	
			lUserTrains[key] = nTrain

	#Label the relationships		
	with open(i_LabeledRelationships, 'a+') as fout:
		with open(i_relationships, newline='') as f:
			reader = csv.reader(f, delimiter=',')
			for row in reader:
				src_user_id = row[0]
				des_user_id = row[1]
				if lUserTrains[src_user_id]>0:
					label = 1
					newCount = lUserTrains[src_user_id] - 1
					lUserTrains[src_user_id] = newCount
				else:
					label = 0
				fout.writelines(str(src_user_id)+','+str(des_user_id)+','+str(label)+"\n")

def combine_post_files(i_directory,o_posts):
	with open(o_posts, 'a+', encoding='utf-8') as fout:
		for filename in os.listdir(i_directory):
			with open(i_directory + '/'+ filename, encoding='utf-8') as f:
				reader = csv.reader(f, delimiter=',')
				for row in reader:
					uid = row[0]
					text = row[1]
					fout.writelines(str(uid)+","+str(text)+"\n")

def combine_post_count_files(i_directory,o_postcounts):
	dPosts = {}
	for filename in os.listdir(i_directory):
		with open(i_directory + '/'+ filename, encoding='utf-8') as f:
			reader = csv.reader(f, delimiter=',')
			for row in reader:
				uid = str(row[0])
				count = int(row[1])
				if uid in dPosts:
					dPosts[uid] = dPosts[uid] + count 
				else:
					dPosts[uid] = count
	with open(o_postcounts, 'a+', encoding='utf-8') as fout:
		for uid in dPosts:
			fout.writelines(str(uid)+","+str(dPosts[uid])+"\n")

def retrieve_user_article_stats(i_userArticles, o_userArticlesStats):
	dPosts = {}
	with open(i_userArticles, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			articleId = str(row[0])
			uid = str(row[1])
			if uid in dPosts:
				dPosts[uid] = dPosts[uid] + 1
			else:
				dPosts[uid] = 1
	with open(o_userArticlesStats, 'a+',encoding='utf-8') as fout:
		for uid in dPosts:
			fout.writelines(str(uid)+','+str(dPosts[uid])+"\n")

def retrieve_acm_posts(i_users, i_userArticles, i_articles, o_posts):
	lUsers = []
	articleDict = {}
	with open(i_articles, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			bibno = row[0]
			text = str(row[1]).strip()
			text = text.replace('\n',' ')
			text = text.replace('\r',' ')
			text = text.lower()
			text = remove_non_alpa(text)
			articleDict[bibno] = text.strip()

	with open(i_users, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = row[0]
			lUsers.append(uid)

	with open(o_posts, 'a+',encoding='utf-8') as fout:
		with open(i_userArticles, newline='',encoding='utf-8') as f:
			reader = csv.reader(f, delimiter=',')
			for row in reader:
				bibno = row[0]
				author = row[1]
				if author in lUsers:
					if bibno in articleDict:
						fout.writelines(author + ","+ articleDict[bibno] +'\n')

def relabel_new_user_relationships(i_new_users, i_relationships, o_relationships):
	lNewUsers = []
	with open(i_new_users, newline='',encoding='utf-8') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = row[0]
			lNewUsers.append(uid)
	with open(o_relationships, 'a+') as fout:
		with open(i_relationships, newline='') as f:
			reader = csv.reader(f, delimiter=',')
			for row in reader:
				src_uid = str(row[0])
				des_uid = str(row[1])
				label = str(row[2])
				if src_uid in lNewUsers or des_uid in lNewUsers:
					fout.writelines(src_uid+','+des_uid+',0\n')
				else:
					fout.writelines(src_uid+','+des_uid+','+label+'\n')

def convert_relationship_to_WTFW(users, relationships, wtfw_links_train, wtfw_links_validation,i_label):
	d_users = {}

	with open(users, newline='') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = str(row[0])
			uindex = int(row[1])
			d_users[uid] = uindex

	with open(wtfw_links_train, 'a+') as foutTrain:
		with open(wtfw_links_validation, 'a+') as foutValid:
			with open(relationships, newline='') as f:
				reader = csv.reader(f, delimiter=',')
				for row in reader:
					src_uid = row[0]
					des_uid = row[1]
					label = row[2]
					if label == '1':
						foutTrain.writelines(str(d_users[src_uid])+'\t'+str(d_users[des_uid])+'\n')
					else:
						foutValid.writelines(str(d_users[src_uid])+'\t'+str(d_users[des_uid])+'\t'+i_label+'\n')

def convert_posts_to_WTFW(users, posts,wtfw_features_train,wtfw_features_validation):
	d_users = {}

	with open(users, newline='') as f:
		reader = csv.reader(f, delimiter=',')
		for row in reader:
			uid = str(row[0])
			uindex = int(row[1])
			d_users[uid] = uindex

	with open(wtfw_features_train, 'a+') as foutTrain:
		with open(wtfw_features_validation, 'a+') as foutValid:
			with open(posts, newline='') as f:
				reader = csv.reader(f, delimiter=',')
				for row in reader:
					uid = row[1]
					post = row[2]
					label = row[3]
					words = post.split(' ')
					if label == '1':
						for word in words:
							if word.strip() != '':
								foutTrain.writelines(str(d_users[uid])+'\t'+word+'\n')
					else:
						for word in words:
							if word.strip() != '':
								foutValid.writelines(str(d_users[uid])+'\t'+word+'\n')


if __name__ == "__main__":

	#test_json('E:/es_release/plr_sg_insta_media_201508/StorageArray3/pprasetyo/plr_sg_insta_media_201508.json',
	#	'F:/Users/roylee/PythonSandbox/Data/test.txt')
	
	#===================================== TWITTER =====================================

	#retrieve_twt_relationship_stats_JSON('E:/es_release/plr_sg_relationship_304/home/jay/services/schedule/relationship/results/plr_sg_relationship_304.json', 20, 20, 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users_follower_counts.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users_followee_counts.csv')

	#retrieve_twt_posts_JSON('E:/es_release/plr_sg_tweet_201610.json', 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users.csv',
	#	200, 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users_post_counts.csv')

	#retrieve_twt_posts_JSON('E:/es_release/plr_sg_tweet_201610.json', 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users1.csv',
	#	200, 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users_post_counts1.csv')

	#retrieve_twt_relationship_JSON('E:/es_release/plr_sg_relationship_304/home/jay/services/schedule/relationship/results/plr_sg_relationship_304.json',
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_relationships.csv')

	#get_rare_words('F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts2.csv', 
	#'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_rare_words.txt', 3)

	#get_common_words('F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts2.csv', 
	#'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_common_words.txt', 500)

	#update_stopwords('F:/Users/roylee/PythonSandbox/Data/ctlr_stopwords_basic.txt',
	#'F:/Users/roylee/PythonSandbox/Data/twitter/twt_rare_words.txt',
	#'F:/Users/roylee/PythonSandbox/Data/twitter/ctlr_twitter_stopwords.txt')

	#remove_stopwords('F:/Users/roylee/PythonSandbox/Data/twitter/ctlr_twitter_stopwords.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts3.csv')

	#retrieve_posts_stats('F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts3.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users2.csv',
	#	 'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_post_stats.csv')

	#filter_posts_by_users('F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts4.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users4.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts5.csv')

	#ilter_relationships_by_users('F:/Users/roylee/PythonSandbox/Data/Twitter/twt_relationships1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users4.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_relationships2.csv')

	#retrieve_relationships_stats('F:/Users/roylee/PythonSandbox/Data/Twitter/twt_relationships2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users4.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users_follower_counts2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users_followee_counts2.csv')

	#get_vocabulary('F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts5.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_vocabulary.csv')

	#convert_post_word_to_index('F:/Users/roylee/PythonSandbox/Data/Twitter/twt_vocabulary.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts6.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts_indexed.csv')

	#label_batch_posts('F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts_indexed.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts_labeled.csv')

	#generate_non_relationships2('F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users4.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_relationships2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_non_relationships.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/Twitter/twt_relationships2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_relationships_labeled.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/Twitter/twt_non_relationships.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_non_relationships_labeled.csv')

	#remove_empty_posts('F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts5.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts6.csv')

	#retrieve_posts_stats('F:/Users/roylee/PythonSandbox/Data/Twitter/twt_posts2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_users4.csv',
	#	 'F:/Users/roylee/PythonSandbox/Data/Twitter/twt_post_counts2.csv')

	#===================================== INSTAGRAM =====================================
	
	#retrieve_ins_users_JSON('E:/es_release/plr_sg_insta_relationship_129/data/instagram/plr_sg_insta_relationship_129.json', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_users.csv')

	#retrieve_ins_relationship_stats_JSON('E:/es_release/plr_sg_insta_relationship_129/data/plr_sg_insta_relationship_129.json', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_users2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_follower_counts2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_followee_counts2.csv')

	#remove_duplicate_relationships('F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships3.csv')

	#retrieve_relationships_stats('F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships3.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_users2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_follower_counts3.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_followee_counts3.csv')

	#retrieve_ins_posts_JSON('E:/es_release/plr_sg_insta_media_201508/StorageArray3/pprasetyo/plr_sg_insta_media_201508.json', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_users2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_posts_aug.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_post_counts_aug.csv')

	#retrieve_ins_posts_JSON('E:/es_release/plr_sg_insta_media_201507/StorageArray3/pprasetyo/plr_sg_insta_media_201507.json', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_users2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_posts_jul.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_post_counts_jul.csv')

	#retrieve_ins_posts_JSON('E:/es_release/plr_sg_insta_media_201506/StorageArray3/pprasetyo/plr_sg_insta_media_201506.json', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_users2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_posts_jun.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_post_counts_jun.csv')

	#filter_relationships_by_users('F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_users2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships2.csv')

	#retrieve_ins_posts_all_users_JSON('E:/es_release/plr_sg_insta_media_201502/StorageArray3/pprasetyo/plr_sg_insta_media_201502.json', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_posts_201502.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_post_counts_201502.csv')

	#retrieve_ins_posts_all_users_JSON('E:/es_release/plr_sg_insta_media_201503/StorageArray3/pprasetyo/plr_sg_insta_media_201503.json', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_posts_201503.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_post_counts_201503.csv')

	#retrieve_ins_posts_all_users_JSON('E:/es_release/plr_sg_insta_media_201504/StorageArray3/pprasetyo/plr_sg_insta_media_201504.json', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_posts_201504.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_post_counts_201504.csv')

	#retrieve_ins_posts_all_users_JSON('E:/es_release/plr_sg_insta_media_201505/StorageArray3/pprasetyo/plr_sg_insta_media_201505.json', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_posts_201505.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_post_counts_201505.csv')

	#retrieve_ins_relationship_stats_JSON('E:/es_release/plr_sg_insta_relationship_129/data/plr_sg_insta_relationship_129.json', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_users9.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships14.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_follower_counts14.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_followee_counts14.csv')

	#remove_duplicate_relationships('F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships14.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships15.csv')
	
	#retrieve_relationships_stats('F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships15.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_users9.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationship_counts15.csv')

	#combine_post_files('F:/Users/roylee/PythonSandbox/Data/instagram/posts',
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_posts.csv')

	#combine_post_count_files('F:/Users/roylee/PythonSandbox/Data/instagram/postcounts',
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_post_counts.csv')

	#filter_posts_by_users('F:/Users/roylee/PythonSandbox/Data/instagram/ins_posts3.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_users9.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_posts6.csv')

	#get_rare_words('F:/Users/roylee/PythonSandbox/Data/Instagram/ins_posts4.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_rare_words1.txt', 3)

	#get_common_words('F:/Users/roylee/PythonSandbox/Data/Instagram/ins_posts4.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_common_words1.txt', 500)

	#update_stopwords('F:/Users/roylee/PythonSandbox/Data/Instagram/ctlr_instagram_stopwords.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_rare_words1.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ctlr_instagram_stopwords1.txt')

	#remove_stopwords('F:/Users/roylee/PythonSandbox/Data/instagram/ctlr_instagram_stopwords1.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_posts4.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_posts5.csv')

	#retrieve_posts_stats('F:/Users/roylee/PythonSandbox/Data/instagram/ins_posts3.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_users9.csv',
	#	 'F:/Users/roylee/PythonSandbox/Data/instagram/ins_post_stats3.csv')

	#get_vocabulary('F:/Users/roylee/PythonSandbox/Data/Instagram/ins_posts6.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_vocabulary.csv')

	#convert_post_word_to_index('F:/Users/roylee/PythonSandbox/Data/Instagram/ins_vocabulary.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_posts6.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_posts_indexed.csv')

	#label_batch_posts('F:/Users/roylee/PythonSandbox/Data/Instagram/ins_posts_indexed.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_posts_labeled.csv')

	
	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/Instagram/ins_relationships15.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships_labeled.csv')

	#generate_non_relationships_random('F:/Users/roylee/PythonSandbox/Data/instagram/ins_users9.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships15.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_random.csv')

	#generate_non_relationships_followee('F:/Users/roylee/PythonSandbox/Data/instagram/ins_users9.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships15.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_followee.csv')

	#generate_non_relationships_popular('F:/Users/roylee/PythonSandbox/Data/instagram/ins_users9.csv', 200,
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships15.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_popular.csv')

	#remove_self_link('F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_random.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_random1.csv')
	
	#remove_self_link('F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_followee.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_followee1.csv')
	
	#remove_self_link('F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_popular.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_popular1.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/instagram/ins_non_relationships_random1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_random_labeled.csv')
	
	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/instagram/ins_non_relationships_followee1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_followee_labeled.csv')
	
	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/instagram/ins_non_relationships_popular1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_popular_labeled.csv')

	#generate_non_relationships_combined('F:/Users/roylee/PythonSandbox/Data/instagram/ins_users9.csv', 0.6, 0.4,
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_popular1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_followee1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_relationships15.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_combined.csv')

	#remove_self_link('F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_combined.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_combined1.csv')

	#retrieve_relationships_stats('F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_combined1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/instagram/ins_users9.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_combined_count1.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_combined1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_combined_labeled.csv')

	#================================== ACM DATA MINING ===================================

	#get_rare_words('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_posts_DM.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acmdm_rare_words.txt', 10)

	#get_common_words('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_posts_DM.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acmdm_common_words.txt', 500)

	#update_stopwords('F:/Users/roylee/PythonSandbox/Data/ctlr_stopwords_basic.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acmdm_rare_words.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/ctlr_acmdm_stopwords_basic.txt')

	#remove_stopwords('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/ctlr_acmdm_stopwords_basic.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_posts_DM.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_posts_DM1.csv')

	#retrieve_posts_stats('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_posts_DM1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_users_DM.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_post_stats_DM.csv')

	#remove_duplicate_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_relationship_DM1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_relationship_DM2.csv')

	#filter_relationships_by_users('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_relationship_DM2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_users_DM2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_relationship_DM3.csv')

	#filter_posts_by_users('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_posts_DM1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_users_DM2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_posts_DM2.csv')

	#retrieve_relationships_stats('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_relationship_DM2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_users_DM2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acmdm_users_follower_counts2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acmdm_users_followee_counts2.csv')

	#get_vocabulary('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_posts_DM2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_vocabulary_DM.csv')

	#convert_post_word_to_index('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_vocabulary_DM.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_posts_DM2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_posts_indexed_DM.csv')

	#label_batch_posts('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_posts_indexed_DM.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_posts_labeled_DM.csv')

	#generate_non_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_users_DM2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_relationship_DM3.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_non_relationship_DM.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_relationship_DM3.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_relationships_labeled_DM.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_non_relationship_DM.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_non_relationships_labeled_DM.csv')

	#===================== ACM DATA MINING ARTIFICIAL INTELLIGENCE ========================
	#get_rare_words('F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_posts_DMAI.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acmdmai_rare_words.txt', 10)

	#get_common_words('F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_posts_DMAI.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acmdmai_common_words.txt', 500)

	#update_stopwords('F:/Users/roylee/PythonSandbox/Data/ctlr_stopwords_basic.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acmdmai_rare_words.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/ctlr_acmdmai_stopwords_basic.txt')

	#remove_stopwords('F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/ctlr_acmdmai_stopwords_basic.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_posts_DMAI.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_posts_DMAI1.csv')

	#remove_duplicate_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_relationship_DMAI.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_relationship_DMAI1.csv')

	#retrieve_posts_stats('F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_posts_DMAI1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_users_DMAI.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_post_stats_DMAI.csv')

	#filter_relationships_by_users('F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_relationship_DMAI2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_users_DMAI2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_relationship_DMAI3.csv')

	#filter_posts_by_users('F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_posts_DMAI1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_users_DMAI2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_posts_DMAI2.csv')

	#retrieve_relationships_stats('F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_relationship_DMAI2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_users_DMAI1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acmdmai_users_follower_counts.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acmdmai_users_followee_counts.csv')

	#get_vocabulary('F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_posts_DMAI2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_vocabulary_DMAI.csv')

	#convert_post_word_to_index('F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_vocabulary_DMAI.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_posts_DMAI2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_posts_indexed_DMAI.csv')

	#label_batch_posts('F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_posts_indexed_DMAI.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_posts_labeled_DMAI.csv')

	#generate_non_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_users_DMAI2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_relationship_DMAI3.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_non_relationship_DMAI.csv')

	#generate_non_relationships2('F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_users_DMAI2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_relationship_DMAI3.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_non_relationship_DMAI1.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_relationship_DMAI3.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dmai/acm_relationships_labeled_DMAI.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_non_relationship_DM.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_dm/acm_non_relationships_labeled_DM.csv')

	#================================== ACM GOOGLE SCHOLAR ===================================

	#get_rare_words('F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_posts_GS.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acmgs_rare_words.txt', 5)

	#get_common_words('F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_posts_GS.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acmgs_common_words.txt', 500)

	#update_stopwords('F:/Users/roylee/PythonSandbox/Data/ctlr_stopwords_basic.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acmgs_rare_words.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/ctlr_acmgs_stopwords_basic.txt')

	#remove_stopwords('F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/ctlr_acmgs_stopwords_basic.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_posts_GS.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_posts_GS1.csv')

	#remove_duplicate_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_relationships_gs.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_relationships_gs1.csv')

	#filter_relationships_by_users('F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_relationships_GS.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_users_GS.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_relationships_GS1.csv')

	#filter_posts_by_users('F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_posts_GS.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_users_GS.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_posts_GS1.csv')

	#retrieve_posts_stats('F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_posts_GS1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_users_GS.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_post_stats_GS1.csv')

	#retrieve_relationships_stats('F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_relationships_GS1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_users_GS.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acmgs_users_follower_counts.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acmgs_users_followee_counts.csv')

	#get_vocabulary('F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_posts_GS1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_vocabulary_GS.csv')

	#convert_post_word_to_index('F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_vocabulary_GS.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_posts_GS1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_posts_indexed_GS.csv')

	#label_batch_posts('F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_posts_indexed_GS.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_posts_labeled_GS.csv')

	#generate_non_relationships2('F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_users_GS.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_relationships_GS1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_non_relationships_GS.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_relationships_GS1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_relationships_labeled_GS.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_non_relationships_GS.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_gs/acm_non_relationships_labeled_GS.csv')

	#================================== ACM PROLIFIC ===================================

	#retrieve_user_article_stats('F:/Users/roylee/PythonSandbox/Data/acm/acm_user_articles.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_user_articles_counts.csv')

	#remove_self_link('F:/Users/roylee/PythonSandbox/Data/acm/acm_relationship_unique.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_relationship_clean.csv')

	#retrieve_acm_posts('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_users.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_user_articles.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_articles.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_posts.csv')

	#filter_relationships_by_users('F:/Users/roylee/PythonSandbox/Data/acm/acm_relationship_clean.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_users.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationships.csv')

	#get_rare_words('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_posts.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_rare_words.txt', 5)

	#get_common_words('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_posts.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_common_words.txt', 500)

	#update_stopwords('F:/Users/roylee/PythonSandbox/Data/ctlr_stopwords_basic.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_rare_words.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/ctlr_acmpro_20_stopwords_basic.txt')

	#remove_stopwords('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/ctlr_acmpro_50_stopwords_basic.txt',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_posts.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_posts1.csv')

	#retrieve_posts_stats('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_posts1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_users.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_posts_stats.csv')

		
	#retrieve_relationships_stats('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_40_relationships1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_40_users1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_40_follower_counts1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_40_followee_counts1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_40_relationship_counts1.csv')
	#
	#retrieve_relationships_stats('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_30_relationships2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_30_users2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_30_follower_counts2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_30_followee_counts2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_30_relationship_counts2.csv')
	#
	#retrieve_relationships_stats('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationships2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_users2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_follower_counts2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_followee_counts2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationship_counts2.csv')

	#filter_posts_by_users('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_posts2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_users2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_posts3.csv')

	#get_vocabulary('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_posts3.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_vocabulary.csv')

	#convert_post_word_to_index('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_vocabulary.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_posts3.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_posts_indexed.csv')

	#label_batch_posts('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_posts_indexed.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_posts_labeled.csv')

	#generate_non_relationships2('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_users2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationships2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_relationships2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_relationships_labeled.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_labeled.csv')

	#generate_non_relationships_random('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_users1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_relationships1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_random.csv')

	#generate_non_relationships_followee('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_users2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_relationships2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_followee.csv')

	#generate_non_relationships_popular('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_users2.csv', 400,
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_relationships2.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_popular.csv')

	#remove_self_link('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_random.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_random1.csv')
	
	#remove_self_link('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_followee.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_followee1.csv')
	
	#remove_self_link('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_popular.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_popular1.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_random1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_random_labeled.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_followee1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_followee_labeled.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_popular1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_popular_labeled.csv')

	#generate_non_relationships_combined('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_users2.csv', 0.6, 0.4,
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_popular1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_followee1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_relationships2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_combined.csv')

	#remove_self_link('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_combined.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_combined1.csv')

	#retrieve_relationships_stats('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_combined1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_users2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_combined_counts1.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_combined1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_combined_labeled.csv')

	#=== BLOCK 1 ===
	#filter_relationships_by_users('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationships6.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_users7.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationships7.csv')

	#retrieve_relationships_stats('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationships7.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_users7.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationships7_counts.csv')

	#=== BLOCK 2 ===
	#filter_posts_by_users('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_posts2.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_users6.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_posts3.csv')

	#get_vocabulary('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_posts3.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_vocabulary.csv')

	#convert_post_word_to_index('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_vocabulary.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_posts3.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_posts_indexed.csv')

	#label_batch_posts('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_posts_indexed.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_posts_labeled.csv')

	#=== BLOCK 3 ===
	#generate_non_relationships_followee('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_users6.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationships7.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_followee.csv')

	#remove_self_link('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_followee.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_followee1.csv')

	#=== BLOCK 4 ===
	#generate_non_relationships_popular('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_users6.csv', 500,
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationships7.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_popular.csv')

	#remove_self_link('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_popular.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_popular1.csv')

	#=== BLOCK 5 ===
	#generate_non_relationships_combined('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_users6.csv', 0.6, 0.4,
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_popular1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_followee1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationships7.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_combined.csv')

	#remove_self_link('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_combined.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_combined1.csv')

	#retrieve_relationships_stats('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_combined1.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_users6.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_combined_counts1.csv')

	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_combined1.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_combined_labeled.csv')

	#=== BLOCK 6 ===
	#label_batch_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationships7.csv',
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationships_labeled.csv')

	#== re-label new user relationships ===
	#relabel_new_user_relationships('F:/Users/roylee/PythonSandbox/Data/Instagram/ins_users_new.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_relationships_labeled.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_relationships_new_users_labeled.csv')

	#relabel_new_user_relationships('F:/Users/roylee/PythonSandbox/Data/Instagram/ins_users_new.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_combined_labeled.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/Instagram/ins_non_relationships_new_users_combined_labeled.csv')

	#relabel_new_user_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_users_new.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_relationships_labeled.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_relationships_new_users_labeled.csv')

	#relabel_new_user_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_users_new.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_combined_labeled.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_50_non_relationships_new_users_combined_labeled.csv')

	#relabel_new_user_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_40_users_new.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_40_relationships_labeled.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_40_relationships_new_users_labeled.csv')

	#relabel_new_user_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_40_users_new.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_40_non_relationships_combined_labeled.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_40_non_relationships_new_users_combined_labeled.csv')

	#relabel_new_user_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_30_users_new.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_30_relationships_labeled.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_30_relationships_new_users_labeled.csv')

	#relabel_new_user_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_30_users_new.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_30_non_relationships_combined_labeled.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_30_non_relationships_new_users_combined_labeled.csv')

	#relabel_new_user_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_users_new.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationships_labeled.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_relationships_new_users_labeled.csv')

	#relabel_new_user_relationships('F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_users_new.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_combined_labeled.csv', 
	#	'F:/Users/roylee/PythonSandbox/Data/acm/acm_pro/acmpro_20_non_relationships_new_users_combined_labeled.csv')

	#================================== WTFW ===================================
	#convert_relationship_to_WTFW('F:/Users/roylee/CTLR/data/acmpro50/with_new_users/users.csv',
	#	'F:/Users/roylee/CTLR/data/acmpro50/with_new_users/nonrelationships.csv',
	#	'F:/Users/roylee/CTLR/data/acmpro50/with_new_users/wtfw_acm50_nonlinks_train.txt',
	#	'F:/Users/roylee/CTLR/data/acmpro50/with_new_users/wtfw_acm50_nonlinks_valid.txt',
	#	'0')
	convert_posts_to_WTFW('F:/Users/roylee/CTLR/data/acmpro50/with_new_users/users.csv',
		'F:/Users/roylee/CTLR/data/acmpro50/with_new_users/posts.csv',
		'F:/Users/roylee/CTLR/data/acmpro50/with_new_users/wtfw_acm50_features_train.txt',
		'F:/Users/roylee/CTLR/data/acmpro50/with_new_users/wtfw_acm50_features_valid.txt')